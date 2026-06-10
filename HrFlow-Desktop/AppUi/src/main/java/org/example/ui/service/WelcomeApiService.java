package org.example.ui.service;

import javafx.concurrent.Task;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Service d'agrégation des APIs externes pour les dashboards Welcome.
 * Même logique que le web (ExternalApiService.php) avec les mêmes clés.
 *
 * APIs utilisées :
 *  - OpenWeatherMap (météo + prévisions) — clé : your_openweather_api_key_here
 *  - GNews (actualités)                  — clé : your_gnews_api_key_here
 *  - open.er-api.com (taux de change)    — sans clé
 *  - quotable.io (citations)             — sans clé
 *  - adviceslip.com (conseils)           — sans clé
 *  - nager.at (jours fériés)             — sans clé (déjà dans PublicHolidayService)
 */
public class WelcomeApiService {

    // ── API Keys ──────────────────────────────────────────────────────────────
    private static final String OPENWEATHER_KEY = "your_openweather_api_key_here";
    private static final String GNEWS_KEY       = "your_gnews_api_key_here";

    // ── Coordonnées Tunis ─────────────────────────────────────────────────────
    private static final double LAT = 36.8065;
    private static final double LON = 10.1815;

    // ── Cache en mémoire statique (partagé entre instances) ──────────────────
    private static final Map<String, Object> CACHE   = new ConcurrentHashMap<>();
    private static final Map<String, Long>   EXPIRES = new ConcurrentHashMap<>();

    private static final ExecutorService POOL = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "WelcomeApi");
        t.setDaemon(true);
        return t;
    });

    // ── Données structurées ───────────────────────────────────────────────────

    public record WeatherData(
            String city,
            double temp,
            double feelsLike,
            String description,
            String icon,        // emoji icon
            int    humidity,
            double windSpeed
    ) {}

    public record ForecastDay(
            String date,
            String day,
            double tempMin,
            double tempMax,
            String icon,
            String description
    ) {}

    public record NewsArticle(
            String title,
            String description,
            String url,
            String source,
            String publishedAt
    ) {}

    public record ExchangeRate(
            String code,
            String symbol,
            String label,
            double rate          // 1 unité = X TND
    ) {}

    public record HolidayEntry(
            String date,
            String name,
            int    daysUntil,
            String formatted
    ) {}

    public record QuoteData(
            String content,
            String author
    ) {}

    // ─────────────────────────────────────────────────────────────────────────
    //  MÉTÉO (OpenWeatherMap current)
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public WeatherData getWeather() {
        String key = "weather.tunis";
        Object cached = getCache(key, 1800);
        if (cached != null) return (WeatherData) cached;

        try {
            String url = "https://api.openweathermap.org/data/2.5/weather"
                    + "?lat=" + LAT + "&lon=" + LON
                    + "&units=metric&lang=fr"
                    + "&appid=" + OPENWEATHER_KEY;
            Map<String, Object> data = httpGet(url);

            @SuppressWarnings("unchecked")
            Map<String, Object> main = (Map<String, Object>) data.get("main");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> weatherList = (List<Map<String, Object>>) data.get("weather");
            @SuppressWarnings("unchecked")
            Map<String, Object> wind = (Map<String, Object>) data.getOrDefault("wind", Map.of());

            String iconCode = (weatherList != null && !weatherList.isEmpty())
                    ? (String) weatherList.get(0).getOrDefault("icon", "01d") : "01d";
            String description = (weatherList != null && !weatherList.isEmpty())
                    ? capitalize((String) weatherList.get(0).getOrDefault("description", "")) : "";

            WeatherData w = new WeatherData(
                    (String) data.getOrDefault("name", "Tunis"),
                    toDouble(main != null ? main.get("temp") : 0),
                    toDouble(main != null ? main.get("feels_like") : 0),
                    description,
                    owmIconToEmoji(iconCode),
                    toInt(main != null ? main.get("humidity") : 0),
                    toDouble(wind.getOrDefault("speed", 0))
            );
            putCache(key, w, 1800);
            return w;
        } catch (Exception e) {
            System.err.println("WelcomeApi – météo: " + e.getMessage());
            return new WeatherData("Tunis", 24, 24, "Indisponible", "☀", 50, 0);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PRÉVISIONS 5 JOURS (OpenWeatherMap forecast)
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<ForecastDay> getForecast() {
        String key = "forecast.tunis";
        Object cached = getCache(key, 3600);
        if (cached != null) return (List<ForecastDay>) cached;

        try {
            String url = "https://api.openweathermap.org/data/2.5/forecast"
                    + "?lat=" + LAT + "&lon=" + LON
                    + "&units=metric&lang=fr"
                    + "&appid=" + OPENWEATHER_KEY;
            Map<String, Object> data = httpGet(url);
            List<Map<String, Object>> list = (List<Map<String, Object>>) data.getOrDefault("list", List.of());

            Map<String, double[]>  minMax = new LinkedHashMap<>();
            Map<String, String>    icons  = new LinkedHashMap<>();
            Map<String, String>    descs  = new LinkedHashMap<>();

            String[] frDays = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};

            for (Map<String, Object> row : list) {
                long   dt      = toLong(row.get("dt"));
                LocalDate date = LocalDate.ofEpochDay(dt / 86400);
                String dateKey = date.toString();

                Map<String, Object> mainMap  = (Map<String, Object>) row.getOrDefault("main", Map.of());
                List<Map<String, Object>> wl = (List<Map<String, Object>>) row.getOrDefault("weather", List.of());
                String icon = wl.isEmpty() ? "01d" : (String) wl.get(0).getOrDefault("icon", "01d");
                String desc = wl.isEmpty() ? "" : capitalize((String) wl.get(0).getOrDefault("description", ""));

                double tmin = toDouble(mainMap.get("temp_min"));
                double tmax = toDouble(mainMap.get("temp_max"));

                minMax.computeIfAbsent(dateKey, k -> new double[]{999, -999});
                if (tmin < minMax.get(dateKey)[0]) minMax.get(dateKey)[0] = tmin;
                if (tmax > minMax.get(dateKey)[1]) minMax.get(dateKey)[1] = tmax;

                icons.putIfAbsent(dateKey, icon);
                descs.putIfAbsent(dateKey, desc);

                // Point à midi → meilleure représentation
                String dtStr = new java.sql.Timestamp(dt * 1000L).toString();
                if (dtStr.contains("12:")) {
                    icons.put(dateKey, icon);
                    descs.put(dateKey, desc);
                }
            }

            List<ForecastDay> result = new ArrayList<>();
            for (Map.Entry<String, double[]> e : minMax.entrySet()) {
                String dateKey = e.getKey();
                LocalDate date = LocalDate.parse(dateKey);
                String day = frDays[date.getDayOfWeek().getValue() - 1];
                result.add(new ForecastDay(
                        dateKey, day,
                        Math.round(e.getValue()[0] * 10.0) / 10.0,
                        Math.round(e.getValue()[1] * 10.0) / 10.0,
                        owmIconToEmoji(icons.getOrDefault(dateKey, "01d")),
                        descs.getOrDefault(dateKey, "")
                ));
                if (result.size() == 5) break;
            }
            putCache(key, result, 3600);
            return result;
        } catch (Exception e) {
            System.err.println("WelcomeApi – prévisions: " + e.getMessage());
            return List.of();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TAUX DE CHANGE (open.er-api.com, base TND)
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<ExchangeRate> getExchangeRates() {
        String key = "rates.tnd";
        Object cached = getCache(key, 21600);
        if (cached != null) return (List<ExchangeRate>) cached;

        try {
            Map<String, Object> data = httpGet("https://open.er-api.com/v6/latest/TND");
            Map<String, Object> rawRates = (Map<String, Object>) data.getOrDefault("rates", Map.of());

            List<ExchangeRate> result = new ArrayList<>();
            String[][] pairs = {
                {"EUR", "€", "Euro"},
                {"USD", "$", "Dollar US"},
                {"GBP", "£", "Livre Sterling"},
                {"CHF", "CHF", "Franc Suisse"}
            };
            for (String[] p : pairs) {
                Object rateObj = rawRates.get(p[0]);
                if (rateObj == null) continue;
                double rawRate = toDouble(rateObj);
                if (rawRate <= 0) continue;
                double inverse = 1.0 / rawRate; // 1 devise = X TND
                result.add(new ExchangeRate(p[0], p[1], p[2], Math.round(inverse * 1000.0) / 1000.0));
            }
            putCache(key, result, 21600);
            return result;
        } catch (Exception e) {
            System.err.println("WelcomeApi – taux: " + e.getMessage());
            return List.of();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ACTUALITÉS (GNews)
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<NewsArticle> getNews(String topic) {
        String key = "news." + topic.hashCode();
        Object cached = getCache(key, 3600);
        if (cached != null) return (List<NewsArticle>) cached;

        try {
            String url = "https://gnews.io/api/v4/search"
                    + "?q=" + urlEncode(topic)
                    + "&lang=fr&country=fr&max=5"
                    + "&apikey=" + GNEWS_KEY;
            Map<String, Object> data = httpGet(url);
            List<Map<String, Object>> articles = (List<Map<String, Object>>) data.getOrDefault("articles", List.of());

            List<NewsArticle> result = new ArrayList<>();
            for (Map<String, Object> a : articles) {
                @SuppressWarnings("unchecked")
                Map<String, Object> source = (Map<String, Object>) a.getOrDefault("source", Map.of());
                result.add(new NewsArticle(
                        (String) a.getOrDefault("title", ""),
                        (String) a.getOrDefault("description", ""),
                        (String) a.getOrDefault("url", "#"),
                        (String) source.getOrDefault("name", "Source inconnue"),
                        (String) a.getOrDefault("publishedAt", "")
                ));
            }
            if (result.isEmpty()) result = newsFallback(topic);
            putCache(key, result, 3600);
            return result;
        } catch (Exception e) {
            System.err.println("WelcomeApi – news: " + e.getMessage());
            return newsFallback(topic);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CITATIONS (quotable.io)
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public QuoteData getQuote(String tag) {
        String key = "quote." + tag;
        Object cached = getCache(key, 21600);
        if (cached != null) return (QuoteData) cached;

        try {
            String url = "https://api.quotable.io/random?tags=" + urlEncode(tag) + "&maxLength=160";
            Map<String, Object> data = httpGet(url);
            String content = (String) data.get("content");
            if (content == null || content.isBlank()) return quoteFallback(tag);
            QuoteData q = new QuoteData(content, (String) data.getOrDefault("author", "Anonyme"));
            putCache(key, q, 21600);
            return q;
        } catch (Exception e) {
            System.err.println("WelcomeApi – citation: " + e.getMessage());
            return quoteFallback(tag);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CONSEIL DU JOUR (adviceslip.com)
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public String getAdvice() {
        String key = "advice." + LocalDate.now();
        Object cached = getCache(key, 21600);
        if (cached != null) return (String) cached;

        try {
            Map<String, Object> data = httpGet("https://api.adviceslip.com/advice");
            @SuppressWarnings("unchecked")
            Map<String, Object> slip = (Map<String, Object>) data.get("slip");
            String advice = slip != null ? (String) slip.get("advice") : null;
            if (advice == null || advice.isBlank()) return adviceFallback();
            putCache(key, advice, 21600);
            return advice;
        } catch (Exception e) {
            System.err.println("WelcomeApi – conseil: " + e.getMessage());
            return adviceFallback();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  JOURS FÉRIÉS TN (nager.at)
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<HolidayEntry> getUpcomingHolidaysTN() {
        String year = String.valueOf(LocalDate.now().getYear());
        String key  = "holidays.tn." + year;
        Object cached = getCache(key, 86400);
        if (cached != null) return (List<HolidayEntry>) cached;

        List<Map<String, Object>> allRaw = new ArrayList<>();
        for (int y = LocalDate.now().getYear(); y <= LocalDate.now().getYear() + 1; y++) {
            try {
                List<?> list = (List<?>) httpGetList("https://date.nager.at/api/v3/PublicHolidays/" + y + "/TN");
                for (Object o : list) allRaw.add((Map<String, Object>) o);
            } catch (Exception e) {
                System.err.println("WelcomeApi – fériés " + y + ": " + e.getMessage());
            }
        }

        String[] frMonths = {"janvier","février","mars","avril","mai","juin",
                "juillet","août","septembre","octobre","novembre","décembre"};

        LocalDate today = LocalDate.now();
        List<HolidayEntry> result = new ArrayList<>();
        for (Map<String, Object> h : allRaw) {
            String dateStr = (String) h.get("date");
            if (dateStr == null) continue;
            try {
                LocalDate dt = LocalDate.parse(dateStr);
                if (dt.isBefore(today)) continue;
                long daysUntil = today.until(dt, java.time.temporal.ChronoUnit.DAYS);
                String formatted = dt.getDayOfMonth() + " " + frMonths[dt.getMonthValue() - 1] + " " + dt.getYear();
                String name = (String) h.getOrDefault("localName", h.getOrDefault("name", "Jour férié"));
                result.add(new HolidayEntry(dateStr, name, (int) daysUntil, formatted));
            } catch (Exception ignored) {}
        }
        result.sort(Comparator.comparingInt(HolidayEntry::daysUntil));
        List<HolidayEntry> top3 = result.stream().limit(3).toList();
        putCache(key, top3, 86400);
        return top3;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CHARGEMENT ASYNCHRONE (pour ne pas bloquer le thread JavaFX)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Charge toutes les données API pour un dashboard "Admin/RH" en arrière-plan.
     * Appelle le callback sur le thread appelant (à wraper avec Platform.runLater si besoin).
     */
    public record DashboardData(
            WeatherData        weather,
            List<ForecastDay>  forecast,
            List<ExchangeRate> rates,
            List<NewsArticle>  news,
            List<HolidayEntry> holidays,
            QuoteData          quote,
            String             advice
    ) {}

    public Task<DashboardData> loadDashboardDataAsync(String newsTopic, String quoteTag) {
        return new Task<>() {
            @Override
            protected DashboardData call() {
                WeatherData        weather  = getWeather();
                List<ForecastDay>  forecast = getForecast();
                List<ExchangeRate> rates    = getExchangeRates();
                List<NewsArticle>  news     = getNews(newsTopic);
                List<HolidayEntry> holidays = getUpcomingHolidaysTN();
                QuoteData          quote    = getQuote(quoteTag);
                String             advice   = getAdvice();
                return new DashboardData(weather, forecast, rates, news, holidays, quote, advice);
            }
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS INTERNES
    // ─────────────────────────────────────────────────────────────────────────

    /** GET HTTP → parse JSON minimal (sans lib externe) */
    @SuppressWarnings("unchecked")
    private Map<String, Object> httpGet(String urlStr) throws Exception {
        String json = fetch(urlStr);
        return (Map<String, Object>) MiniJson.parse(json);
    }

    @SuppressWarnings("unchecked")
    private List<?> httpGetList(String urlStr) throws Exception {
        String json = fetch(urlStr);
        return (List<?>) MiniJson.parse(json);
    }

    private String fetch(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "HrFlowJava/1.0");

        int code = conn.getResponseCode();
        var is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (is == null) throw new Exception("No response body (HTTP " + code + ")");

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    // ── Cache ────────────────────────────────────────────────────────────────

    private Object getCache(String key, long ttlSeconds) {
        Long exp = EXPIRES.get(key);
        if (exp == null || System.currentTimeMillis() > exp) return null;
        return CACHE.get(key);
    }

    private void putCache(String key, Object value, long ttlSeconds) {
        CACHE.put(key, value);
        EXPIRES.put(key, System.currentTimeMillis() + ttlSeconds * 1000L);
    }

    // ── Conversions ──────────────────────────────────────────────────────────

    private double toDouble(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (Exception e) { return 0; }
    }
    private int toInt(Object o) { return (int) toDouble(o); }
    private long toLong(Object o) {
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return 0L; }
    }
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
    private String urlEncode(String s) {
        return s.replace(" ", "%20").replace("é", "%C3%A9").replace("è", "%C3%A8");
    }

    // ── OWM icon → emoji ─────────────────────────────────────────────────────
    private String owmIconToEmoji(String icon) {
        if (icon == null) return "☀";
        return switch (icon.substring(0, Math.min(2, icon.length()))) {
            case "01" -> "☀";
            case "02" -> "🌤";
            case "03" -> "⛅";
            case "04" -> "☁";
            case "09" -> "🌧";
            case "10" -> "🌦";
            case "11" -> "⛈";
            case "13" -> "❄";
            case "50" -> "🌫";
            default   -> "🌡";
        };
    }

    // ── Fallbacks ─────────────────────────────────────────────────────────────

    private List<NewsArticle> newsFallback(String topic) {
        return List.of(new NewsArticle(
                "Actualités indisponibles",
                "Le service est momentanément indisponible.",
                "#", "HrFlow", ""
        ));
    }

    private QuoteData quoteFallback(String tag) {
        return switch (tag) {
            case "leadership" -> new QuoteData(
                    "Un leader est celui qui connaît le chemin, qui suit le chemin et qui montre le chemin.",
                    "John C. Maxwell");
            case "business" -> new QuoteData(
                    "Le seul endroit où le succès vient avant le travail, c'est dans le dictionnaire.",
                    "Vidal Sassoon");
            default -> new QuoteData(
                    "Le succès est la somme de petits efforts répétés jour après jour.",
                    "Robert Collier");
        };
    }

    private String adviceFallback() {
        return "Prenez quelques minutes chaque matin pour planifier votre journée — cela change tout.";
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Mini JSON parser (sans dépendance externe)
    // ─────────────────────────────────────────────────────────────────────────

    private static final class MiniJson {
        static Object parse(String json) {
            json = json.trim();
            if (json.startsWith("{")) return parseObject(json, new int[]{0});
            if (json.startsWith("[")) return parseArray(json, new int[]{0});
            return json;
        }

        private static Map<String, Object> parseObject(String s, int[] pos) {
            Map<String, Object> map = new LinkedHashMap<>();
            pos[0]++; // skip {
            while (pos[0] < s.length()) {
                skipWhitespace(s, pos);
                if (s.charAt(pos[0]) == '}') { pos[0]++; break; }
                if (s.charAt(pos[0]) == ',') { pos[0]++; continue; }
                String key = parseString(s, pos);
                skipWhitespace(s, pos);
                if (pos[0] < s.length() && s.charAt(pos[0]) == ':') pos[0]++;
                skipWhitespace(s, pos);
                Object val = parseValue(s, pos);
                map.put(key, val);
            }
            return map;
        }

        private static List<Object> parseArray(String s, int[] pos) {
            List<Object> list = new ArrayList<>();
            pos[0]++; // skip [
            while (pos[0] < s.length()) {
                skipWhitespace(s, pos);
                if (s.charAt(pos[0]) == ']') { pos[0]++; break; }
                if (s.charAt(pos[0]) == ',') { pos[0]++; continue; }
                list.add(parseValue(s, pos));
            }
            return list;
        }

        private static Object parseValue(String s, int[] pos) {
            skipWhitespace(s, pos);
            if (pos[0] >= s.length()) return null;
            char c = s.charAt(pos[0]);
            if (c == '"')  return parseString(s, pos);
            if (c == '{')  return parseObject(s, pos);
            if (c == '[')  return parseArray(s, pos);
            if (c == 't')  { pos[0] += 4; return Boolean.TRUE; }
            if (c == 'f')  { pos[0] += 5; return Boolean.FALSE; }
            if (c == 'n')  { pos[0] += 4; return null; }
            // number
            int start = pos[0];
            while (pos[0] < s.length() && "0123456789.-+eE".indexOf(s.charAt(pos[0])) >= 0) pos[0]++;
            String num = s.substring(start, pos[0]);
            try {
                if (num.contains(".") || num.contains("e") || num.contains("E"))
                    return Double.parseDouble(num);
                return Long.parseLong(num);
            } catch (NumberFormatException e) { return num; }
        }

        private static String parseString(String s, int[] pos) {
            pos[0]++; // skip opening "
            StringBuilder sb = new StringBuilder();
            while (pos[0] < s.length()) {
                char c = s.charAt(pos[0]++);
                if (c == '"') break;
                if (c == '\\' && pos[0] < s.length()) {
                    char esc = s.charAt(pos[0]++);
                    switch (esc) {
                        case '"'  -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/'  -> sb.append('/');
                        case 'n'  -> sb.append('\n');
                        case 'r'  -> sb.append('\r');
                        case 't'  -> sb.append('\t');
                        case 'u'  -> {
                            if (pos[0] + 4 <= s.length()) {
                                String hex = s.substring(pos[0], pos[0] + 4);
                                pos[0] += 4;
                                try { sb.append((char) Integer.parseInt(hex, 16)); }
                                catch (NumberFormatException ignore) { sb.append("\\u").append(hex); }
                            }
                        }
                        default -> sb.append(esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private static void skipWhitespace(String s, int[] pos) {
            while (pos[0] < s.length() && Character.isWhitespace(s.charAt(pos[0]))) pos[0]++;
        }
    }
}
