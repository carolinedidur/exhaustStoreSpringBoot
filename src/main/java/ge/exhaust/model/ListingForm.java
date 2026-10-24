package ge.exhaust.model;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class ListingForm {
    @NotBlank @Size(max=40) private String sku="";
    @NotBlank @Size(max=100) private String title="";
    @NotBlank @Size(max=60) private String brand="";
    @NotBlank @Size(max=60) private String category="Axle-back exhausts";
    @NotBlank @Size(max=3000) private String description="";
    @NotBlank @Size(max=100) private String material="Stainless steel";
    @NotNull @DecimalMin("0.01") @DecimalMax("999999.99") @Digits(integer=6,fraction=2) private BigDecimal price=null;
    @NotNull @Min(0) @Max(9999) private Integer stockGoodwill=0;
    @NotNull @Min(0) @Max(9999) private Integer stockGrmagele=0;
    @NotBlank @Size(max=4000) private String compatibility="";
    @Size(max=1000) @Pattern(regexp="^$|https?://[^\\s]+",message="Enter a full http:// or https:// URL.") private String sourceUrl="";
    @Size(max=200) private String imageCredit="";
     private boolean published=true;
     private boolean featured=false;
    public String getSku(){return sku;}
    public void setSku(String value){this.sku=value;}
    public String getTitle(){return title;}
    public void setTitle(String value){this.title=value;}
    public String getBrand(){return brand;}
    public void setBrand(String value){this.brand=value;}
    public String getCategory(){return category;}
    public void setCategory(String value){this.category=value;}
    public String getDescription(){return description;}
    public void setDescription(String value){this.description=value;}
    public String getMaterial(){return material;}
    public void setMaterial(String value){this.material=value;}
    public BigDecimal getPrice(){return price;}
    public void setPrice(BigDecimal value){this.price=value;}
    public Integer getStockGoodwill(){return stockGoodwill;}
    public void setStockGoodwill(Integer value){this.stockGoodwill=value;}
    public Integer getStockGrmagele(){return stockGrmagele;}
    public void setStockGrmagele(Integer value){this.stockGrmagele=value;}
    public String getCompatibility(){return compatibility;}
    public void setCompatibility(String value){this.compatibility=value;}
    public String getSourceUrl(){return sourceUrl;}
    public void setSourceUrl(String value){this.sourceUrl=value;}
    public String getImageCredit(){return imageCredit;}
    public void setImageCredit(String value){this.imageCredit=value;}
    public boolean isPublished(){return published;}
    public void setPublished(boolean value){this.published=value;}
    public boolean isFeatured(){return featured;}
    public void setFeatured(boolean value){this.featured=value;}
    public List<Fitment> parseFitments() {
        List<Fitment> fits=new ArrayList<>();
        for(String line:compatibility.lines().filter(s->!s.isBlank()).toList()) {
            String[] p=line.split("\\|",-1);
            if(p.length<4 || p.length>5) throw new IllegalArgumentException("Use Make | Model | From year | To year | Engine, one car per line.");
            try {
                int from=Integer.parseInt(p[2].strip()),to=Integer.parseInt(p[3].strip());
                if(p[0].isBlank()||p[1].isBlank()||p[0].length()>60||p[1].length()>80||from<1950||to>2100||from>to) throw new NumberFormatException();
                fits.add(new Fitment(p[0].strip(),p[1].strip(),from,to,p.length==5?p[4].strip():""));
            } catch(NumberFormatException ex) {throw new IllegalArgumentException("Each fitment needs a make, model, and valid years (1950–2100), with From no later than To.");}
        }
        if(fits.isEmpty()||fits.size()>20) throw new IllegalArgumentException("Add between 1 and 20 compatible cars.");
        return fits;
    }
    public static ListingForm from(Product p) {
        ListingForm f=new ListingForm();
        f.sku=p.sku();f.title=p.title();f.brand=p.brand();f.category=p.category();
        f.description=p.description();f.material=p.material();f.price=p.price();
        f.stockGoodwill=p.stockGoodwill();f.stockGrmagele=p.stockGrmagele();f.published=p.published();f.featured=p.featured();
        f.sourceUrl=p.sourceUrl();f.imageCredit=p.imageCredit();
        f.compatibility=p.fitments().stream().map(v->String.join(" | ",v.make(),v.model(),String.valueOf(v.yearFrom()),String.valueOf(v.yearTo()),v.engine())).collect(Collectors.joining("\n"));
        return f;
    }
    public Product product(long id,String image,List<Fitment> fits) {
        return new Product(id,sku.strip(),title.strip(),brand.strip(),category.strip(),description.strip(),material.strip(),
                price.movePointRight(2).intValueExact(),image,sourceUrl.strip(),imageCredit.strip(),stockGoodwill,stockGrmagele,published,featured,fits);
    }
}

