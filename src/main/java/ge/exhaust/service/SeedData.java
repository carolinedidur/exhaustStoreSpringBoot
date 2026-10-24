package ge.exhaust.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ge.exhaust.model.Product;
import ge.exhaust.repository.StoreRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Component
public class SeedData implements ApplicationRunner {
    private final StoreRepository store;
    private final ObjectMapper mapper;
    public SeedData(StoreRepository store,ObjectMapper mapper) {this.store=store;this.mapper=mapper;}
    @Override @Transactional public void run(ApplicationArguments args) throws Exception {
        // A durable marker prevents deleted/hidden demo listings from reappearing on restart.
        if(!store.setting("seed-version","").isEmpty()) return;
        try(var input=new ClassPathResource("seed/products.json").getInputStream()) {
            List<Product> products=mapper.readValue(input,new TypeReference<>(){});
            for(Product product:products) store.save(product);
        }
        store.setSetting("hero-image","/images/bmw-g82.jpg");
        store.setSetting("hero-title","Good cars.\nGreat sound.");
        store.setSetting("hero-description","Exhaust systems, performance parts, and the little things that make a big difference. Right here in Tbilisi.");
        store.setSetting("hero-credit","Photo: Lukász Szabó / Pexels");
        store.setSetting("hero-source","https://www.pexels.com/photo/the-rear-end-of-a-black-car-parked-on-the-side-of-the-road-28209153/");
        store.setSetting("seed-version","1");
    }
}
