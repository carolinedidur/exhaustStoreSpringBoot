package ge.exhaust.controller;

import ge.exhaust.model.CatalogFilter;
import ge.exhaust.repository.StoreRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class StoreController {
    private final StoreRepository store;
    public StoreController(StoreRepository store){this.store=store;}

    @ModelAttribute public void common(Model model) {
        model.addAttribute("vehicles",store.vehicleOptions());
        model.addAttribute("years",store.years());
        model.addAttribute("categories",store.categories());
    }
    @GetMapping("/") public String home(Model model) {
        model.addAttribute("page","home");
        model.addAttribute("heroImage",store.setting("hero-image","/images/bmw-g82.jpg"));
        model.addAttribute("heroLines",store.setting("hero-title","Good cars.\nGreat sound.").split("\\R",2));
        model.addAttribute("heroDescription",store.setting("hero-description","Find your next exhaust upgrade in Tbilisi."));
        var products=store.all(false);
        var featured=products.stream().filter(p->p.featured()).limit(4).toList();
        model.addAttribute("featured",featured.isEmpty()?products.stream().limit(4).toList():featured);
        model.addAttribute("totalProducts",products.size());
        return "home";
    }
    @GetMapping({"/listings","/posts"}) public String listings(
            @RequestParam(required=false) String q,@RequestParam(required=false) String make,
            @RequestParam(required=false) String model,@RequestParam(required=false) Integer year,
            @RequestParam(required=false) String category,@RequestParam(required=false) String storage,
            @RequestParam(required=false) String availability,@RequestParam(required=false) String sort,Model view) {
        CatalogFilter filter=new CatalogFilter(q,make,model,year,category,storage,availability,sort);
        view.addAttribute("page","listings"); view.addAttribute("filter",filter);
        view.addAttribute("products",store.find(filter,false));
        view.addAttribute("totalProducts",store.all(false).size());
        return "listings";
    }
    @GetMapping("/listings/{id}") public String detail(@PathVariable long id,Model model) {
        var product=store.byId(id,false).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("page","listings");model.addAttribute("product",product);
        model.addAttribute("related",store.all(false).stream().filter(p->p.id()!=id && p.category().equals(product.category())).limit(3).toList());
        return "detail";
    }
    @GetMapping("/locations") public String locations(Model model) {model.addAttribute("page","locations");return "locations";}
    @GetMapping("/login") public String login(Model model) {model.addAttribute("page","admin");return "login";}
    @GetMapping("/credits") public String credits(Model model) {
        model.addAttribute("page","credits"); model.addAttribute("products",store.all(false));
        model.addAttribute("heroCredit",store.setting("hero-credit","Uploaded photograph"));
        model.addAttribute("heroSource",store.setting("hero-source",""));return "credits";
    }
}
