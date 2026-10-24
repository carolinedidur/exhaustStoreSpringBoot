package ge.exhaust.repository;

import ge.exhaust.model.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Statement;
import java.util.*;
import java.util.stream.IntStream;

@Repository
public class StoreRepository {
    private final JdbcTemplate jdbc;
    public StoreRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Product> find(CatalogFilter filter, boolean admin) {
        Map<Long,List<Fitment>> fits = new HashMap<>();
        jdbc.query("SELECT * FROM fitments ORDER BY id", rs -> {
            fits.computeIfAbsent(rs.getLong("product_id"), k -> new ArrayList<>()).add(new Fitment(
                    rs.getString("make"),rs.getString("model"),rs.getInt("year_from"),rs.getInt("year_to"),rs.getString("engine")));
        });
        StringBuilder sql = new StringBuilder("SELECT p.* FROM products p WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (!admin) sql.append(" AND p.published=1");
        if (!filter.q().isEmpty()) {
            sql.append(" AND (p.title LIKE ? ESCAPE '\\' OR p.brand LIKE ? ESCAPE '\\' OR p.sku LIKE ? ESCAPE '\\')");
            String term = "%" + filter.q().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            args.add(term); args.add(term); args.add(term);
        }
        if (!filter.category().isEmpty()) { sql.append(" AND p.category=?"); args.add(filter.category()); }
        if ("goodwill".equals(filter.storage())) sql.append(" AND p.stock_goodwill>0");
        if ("grmagele".equals(filter.storage())) sql.append(" AND p.stock_grmagele>0");
        if ("in-stock".equals(filter.availability())) sql.append(" AND (p.stock_goodwill+p.stock_grmagele)>0");
        if ("out-of-stock".equals(filter.availability())) sql.append(" AND (p.stock_goodwill+p.stock_grmagele)=0");
        if (!filter.make().isEmpty() || !filter.model().isEmpty() || filter.year() != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM fitments f WHERE f.product_id=p.id");
            if (!filter.make().isEmpty()) { sql.append(" AND f.make=?"); args.add(filter.make()); }
            if (!filter.model().isEmpty()) { sql.append(" AND f.model=?"); args.add(filter.model()); }
            if (filter.year() != null) { sql.append(" AND f.year_from<=? AND f.year_to>=?"); args.add(filter.year()); args.add(filter.year()); }
            sql.append(")");
        }
        sql.append(switch(filter.sort()) {
            case "price-low" -> " ORDER BY p.price_cents ASC,p.id DESC";
            case "price-high" -> " ORDER BY p.price_cents DESC,p.id DESC";
            case "name" -> " ORDER BY p.title COLLATE NOCASE ASC";
            case "newest" -> " ORDER BY p.id DESC";
            default -> " ORDER BY p.featured DESC,p.id ASC";
        });
        return jdbc.query(sql.toString(), (rs, row) -> new Product(rs.getLong("id"), rs.getString("sku"),
                rs.getString("title"),rs.getString("brand"),rs.getString("category"),rs.getString("description"),
                rs.getString("material"),rs.getInt("price_cents"),rs.getString("image_path"),rs.getString("source_url"),
                rs.getString("image_credit"),rs.getInt("stock_goodwill"),rs.getInt("stock_grmagele"),
                rs.getBoolean("published"),rs.getBoolean("featured"),fits.getOrDefault(rs.getLong("id"),List.of())), args.toArray());
    }

    public List<Product> all(boolean admin) { return find(new CatalogFilter(null,null,null,null,null,null,null,null),admin); }
    public Optional<Product> byId(long id, boolean admin) { return all(admin).stream().filter(p -> p.id()==id).findFirst(); }
    public List<Fitment> fitments(long id) {
        return jdbc.query("SELECT * FROM fitments WHERE product_id=? ORDER BY id", (rs,n) ->
                new Fitment(rs.getString("make"),rs.getString("model"),rs.getInt("year_from"),rs.getInt("year_to"),rs.getString("engine")),id);
    }
    public Map<String,List<String>> vehicleOptions() {
        Map<String,List<String>> vehicles = new TreeMap<>();
        jdbc.query("SELECT DISTINCT f.make,f.model FROM fitments f JOIN products p ON p.id=f.product_id WHERE p.published=1 ORDER BY f.make,f.model",
            rs -> { vehicles.computeIfAbsent(rs.getString("make"), k -> new ArrayList<>()).add(rs.getString("model")); });
        return vehicles;
    }
    public List<Integer> years() {
        List<Fitment> all = all(false).stream().flatMap(p -> p.fitments().stream()).toList();
        if(all.isEmpty()) return List.of();
        int min=all.stream().mapToInt(Fitment::yearFrom).min().orElse(2000);
        int max=all.stream().mapToInt(Fitment::yearTo).max().orElse(2026);
        return IntStream.rangeClosed(min,max).boxed().sorted(Comparator.reverseOrder()).toList();
    }
    public List<String> categories() {
        return jdbc.queryForList("SELECT DISTINCT category FROM products WHERE published=1 ORDER BY category",String.class);
    }
    @Transactional
    public long save(Product p) {
        Object[] values = {p.sku(),p.title(),p.brand(),p.category(),p.description(),p.material(),p.priceCents(),
                p.imagePath(),p.sourceUrl(),p.imageCredit(),p.stockGoodwill(),p.stockGrmagele(),p.published(),p.featured()};
        long id=p.id();
        String columns="sku,title,brand,category,description,material,price_cents,image_path,source_url,image_credit,stock_goodwill,stock_grmagele,published,featured";
        if(id==0) {
            GeneratedKeyHolder keys=new GeneratedKeyHolder();
            jdbc.update(connection -> {
                var statement=connection.prepareStatement("INSERT INTO products ("+columns+") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
                for(int i=0;i<values.length;i++) statement.setObject(i+1,values[i]);
                return statement;
            }, keys);
            id=Objects.requireNonNull(keys.getKey()).longValue();
        } else {
            List<Object> args=new ArrayList<>(Arrays.asList(values)); args.add(id);
            jdbc.update("UPDATE products SET "+String.join("=?,",columns.split(","))+"=? WHERE id=?",args.toArray());
            jdbc.update("DELETE FROM fitments WHERE product_id=?",id);
        }
        for(Fitment f:p.fitments()) jdbc.update("INSERT INTO fitments(product_id,make,model,year_from,year_to,engine) VALUES (?,?,?,?,?,?)",
                id,f.make(),f.model(),f.yearFrom(),f.yearTo(),f.engine());
        return id;
    }
    public boolean skuExists(String sku,long exceptId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM products WHERE sku=? AND id<>?",Integer.class,sku,exceptId)>0;
    }
    public void delete(long id) { jdbc.update("DELETE FROM products WHERE id=?",id); }
    public String setting(String key,String fallback) {
        return jdbc.query("SELECT value FROM settings WHERE key=?", (rs,n)->rs.getString(1),key).stream().findFirst().orElse(fallback);
    }
    public void setSetting(String key,String value) {
        jdbc.update("INSERT INTO settings(key,value) VALUES (?,?) ON CONFLICT(key) DO UPDATE SET value=excluded.value",key,value);
    }
}
