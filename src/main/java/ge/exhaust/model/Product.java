package ge.exhaust.model;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public record Product(long id, String sku, String title, String brand, String category,
                      String description, String material, int priceCents, String imagePath,
                      String sourceUrl, String imageCredit, int stockGoodwill, int stockGrmagele,
                      boolean published, boolean featured, List<Fitment> fitments) {
    public boolean available() { return stockGoodwill + stockGrmagele > 0; }
    public BigDecimal price() { return BigDecimal.valueOf(priceCents, 2); }
    public String formattedPrice() {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        format.setMaximumFractionDigits(2);
        return format.format(price()) + " ₾";
    }
    public String fitmentSummary() {
        return fitments.isEmpty() ? "Custom fitment" : fitments.get(0).label()
                + (fitments.size() > 1 ? " + " + (fitments.size() - 1) + " more" : "");
    }
    public String yearSummary() { return fitments.isEmpty() ? "" : fitments.get(0).years(); }
}
