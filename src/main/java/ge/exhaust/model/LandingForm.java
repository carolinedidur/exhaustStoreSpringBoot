package ge.exhaust.model;

import jakarta.validation.constraints.*;

public class LandingForm {
    @NotBlank @Size(max=80) private String title;
    @NotBlank @Size(max=220) private String description;
    @Size(max=200) private String credit="";
    @Size(max=1000) @Pattern(regexp="^$|https?://[^\\s]+",message="Enter a full http:// or https:// URL.") private String source="";
    public String getTitle(){return title;}
    public void setTitle(String value){title=value;}
    public String getDescription(){return description;}
    public void setDescription(String value){description=value;}
    public String getCredit(){return credit;}
    public void setCredit(String value){credit=value;}
    public String getSource(){return source;}
    public void setSource(String value){source=value;}
}
