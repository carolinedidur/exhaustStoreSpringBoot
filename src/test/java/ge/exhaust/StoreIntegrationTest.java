package ge.exhaust;

import ge.exhaust.model.*;
import ge.exhaust.repository.StoreRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.stream.IntStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StoreIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired StoreRepository store;
    static final Path TEMP;
    static {try{TEMP=Files.createTempDirectory("exhaust-tests-");}catch(Exception e){throw new RuntimeException(e);}}
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",()->"jdbc:sqlite:"+TEMP.resolve("test.db")+"?foreign_keys=on&busy_timeout=5000");
        registry.add("app.upload-dir",()->TEMP.resolve("uploads").toString());
    }
    @Test void seedsThirtySixPartsWithBundledImages() throws Exception {
        assertThat(store.all(false)).hasSize(36);
        for(Product p:store.all(false)) {
            assertThat(new ClassPathResource("static"+p.imagePath()).exists()).isTrue();
            assertThat(p.fitments()).isNotEmpty();assertThat(p.sourceUrl()).startsWith("https://");
            assertThat(p.priceCents()).isPositive();
        }
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(containsString("Good cars.")))
                .andExpect(content().string(containsString("/images/bmw-g82.jpg")));
    }
    @Test void rendersAllPublicPagesAndImages() throws Exception {
        for(String url:List.of("/listings","/posts","/listings/1","/locations","/credits","/login","/css/store.css","/fonts/barlow-regular.woff2"))
            mvc.perform(get(url)).andExpect(status().isOk());
        mvc.perform(get("/listings/999999")).andExpect(status().isNotFound());
    }
    @Test void combinesMakeModelAndYear() throws Exception {
        var filter=new CatalogFilter(null,"BMW","M4 (G82)",2023,null,null,null,null);
        assertThat(store.find(filter,false)).isNotEmpty().allSatisfy(p ->
            assertThat(p.fitments()).anyMatch(f->f.make().equals("BMW")&&f.model().equals("M4 (G82)")&&f.yearFrom()<=2023&&f.yearTo()>=2023));
        mvc.perform(get("/listings").param("make","BMW").param("model","M4 (G82)").param("year","2023"))
            .andExpect(status().isOk()).andExpect(model().attribute("products",hasSize(store.find(filter,false).size())));
        mvc.perform(get("/listings").param("make","Toyota").param("model","M4 (G82)"))
            .andExpect(status().isOk()).andExpect(content().string(containsString("No parts in this lane.")));
    }
    @Test void fitmentConstraintsMustMatchTheSameCar() {
        long id=store.save(new Product(0,"TEST-CROSS","Cross fitment test","Test","Midpipes","Test part","Steel",10000,
                "/images/part-placeholder.svg","","",1,0,true,false,List.of(
                new Fitment("BMW","Test Model",2001,2005,""),new Fitment("Toyota","Test Model",2020,2024,""))));
        assertThat(store.find(new CatalogFilter("Cross fitment", "BMW",null,2022,null,null,null,null),false)).isEmpty();
        assertThat(store.find(new CatalogFilter("Cross fitment", "Toyota",null,2022,null,null,null,null),false)).extracting(Product::id).containsExactly(id);
    }
    @Test void filtersStockPerStorageAndPartType() {
        assertThat(store.find(new CatalogFilter(null,null,null,null,"Midpipes","goodwill","in-stock",null),false))
            .isNotEmpty().allMatch(p->p.stockGoodwill()>0&&p.category().equals("Midpipes"));
        assertThat(store.find(new CatalogFilter(null,null,null,null,null,"grmagele",null,null),false))
            .isNotEmpty().allMatch(p->p.stockGrmagele()>0);
        assertThat(store.find(new CatalogFilter(null,null,null,null,null,null,"out-of-stock",null),false))
            .isNotEmpty().allMatch(p->p.stockGoodwill()==0&&p.stockGrmagele()==0);
    }
    @Test void sortsPricesAndTreatsSearchAsData() {
        List<Product> sorted=store.find(new CatalogFilter(null,null,null,null,null,null,null,"price-low"),false);
        assertThat(IntStream.range(1,sorted.size()).allMatch(i->sorted.get(i-1).priceCents()<=sorted.get(i).priceCents())).isTrue();
        assertThat(store.find(new CatalogFilter("' OR 1=1 --",null,null,null,null,null,null,null),false)).isEmpty();
        assertThat(store.find(new CatalogFilter("%",null,null,null,null,null,null,null),false)).isEmpty();
        assertThat(store.find(new CatalogFilter("EXT-001",null,null,null,null,null,null,null),false)).hasSize(1);
    }
    @Test void adminRequiresLoginAndCsrf() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(post("/admin/listings/1/delete")).andExpect(status().isForbidden());
        mvc.perform(post("/login").with(csrf()).param("username","admin").param("password","admin123"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin"));
        mvc.perform(post("/login").with(csrf()).param("username","admin").param("password","wrong"))
            .andExpect(redirectedUrl("/login?error"));
    }
    @Test @WithMockUser(roles="ADMIN") void adminPagesRender() throws Exception {
        for(String url:List.of("/admin","/admin?state=hidden","/admin/listings/new","/admin/listings/1/edit","/admin/landing","/admin/listings/1/delete"))
            mvc.perform(get(url)).andExpect(status().isOk());
    }
    private MockHttpServletRequestBuilder validForm() {
        return post("/admin/listings/save").with(csrf()).param("sku","TEST-037").param("title","Test stainless midpipe")
            .param("brand","Test Brand").param("category","Midpipes").param("description","An integration test part.")
            .param("material","Stainless steel").param("price","1200.50").param("stockGoodwill","3").param("stockGrmagele","0")
            .param("compatibility","BMW | M4 (G82) | 2021 | 2026 | 3.0 S58").param("sourceUrl","https://example.com/parts")
            .param("imageCredit","Test").param("published","true").param("featured","false");
    }
    private MockHttpServletRequestBuilder override(MockHttpServletRequestBuilder builder,String... pairs) {
        return builder.with(request->{for(int i=0;i<pairs.length;i+=2)request.setParameter(pairs[i],pairs[i+1]);return request;});
    }
    @Test @WithMockUser(roles="ADMIN") void createsEditsHidesAndDeletesListing() throws Exception {
        mvc.perform(validForm()).andExpect(redirectedUrl("/admin"));
        Product created=store.all(true).stream().filter(p->p.sku().equals("TEST-037")).findFirst().orElseThrow();
        assertThat(created.priceCents()).isEqualTo(120050);assertThat(created.stockGoodwill()).isEqualTo(3);
        mvc.perform(override(validForm(),"id",String.valueOf(created.id()),"title","Updated test part"))
                .andExpect(redirectedUrl("/admin"));
        assertThat(store.byId(created.id(),true).orElseThrow().title()).isEqualTo("Updated test part");
        mvc.perform(post("/admin/listings/{id}/visibility",created.id()).with(csrf())).andExpect(redirectedUrl("/admin"));
        assertThat(store.byId(created.id(),false)).isEmpty();
        assertThat(store.byId(created.id(),true)).isPresent();
        mvc.perform(get("/listings/{id}",created.id())).andExpect(status().isNotFound());
        mvc.perform(post("/admin/listings/{id}/delete",created.id()).with(csrf())).andExpect(redirectedUrl("/admin"));
        assertThat(store.byId(created.id(),true)).isEmpty();
        assertThat(store.fitments(created.id())).isEmpty();
    }
    @Test @WithMockUser(roles="ADMIN") void rejectsNegativeStockAndBadFitments() throws Exception {
        mvc.perform(override(validForm(),"stockGoodwill","-1","compatibility","BMW | M4 | 2026 | 2021"))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form","stockGoodwill","compatibility"));
        assertThat(store.all(true)).hasSize(36);
    }
    @Test @WithMockUser(roles="ADMIN") void rejectsDuplicateSkuAndUnsafeSourceLink() throws Exception {
        mvc.perform(override(validForm(),"sku","EXT-001","sourceUrl","javascript:alert(1)"))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form","sku","sourceUrl"));
        assertThat(store.all(true)).hasSize(36);
    }
    @Test @WithMockUser(roles="ADMIN") void landingChangesPersistAndImagesAreValidated() throws Exception {
        var image=new MockMultipartFile("image","landing.png","image/png",Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aK1cAAAAASUVORK5CYII="));
        mvc.perform(multipart("/admin/landing").file(image).with(csrf()).param("title","Fresh sound.")
            .param("description","A new introduction.").param("credit","Photographer").param("source","https://example.com/photo"))
            .andExpect(redirectedUrl("/admin/landing"));
        assertThat(store.setting("hero-title","")).isEqualTo("Fresh sound.");
        String photo=store.setting("hero-image","");assertThat(photo).startsWith("/uploads/");
        assertThat(Files.exists(TEMP.resolve("uploads").resolve(photo.substring("/uploads/".length())))).isTrue();
        mvc.perform(get(photo)).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        mvc.perform(get("/")).andExpect(content().string(containsString("Fresh sound.")));
        mvc.perform(multipart("/admin/landing").file(new MockMultipartFile("image","evil.png","image/png","<script>bad</script>".getBytes()))
            .with(csrf()).param("title","Bad image").param("description","Text").param("credit","").param("source",""))
            .andExpect(status().isOk()).andExpect(model().hasErrors());
        assertThat(store.setting("hero-image","")).isEqualTo(photo);
    }
}
