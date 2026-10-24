package ge.exhaust.controller;

import ge.exhaust.model.*;
import ge.exhaust.repository.StoreRepository;
import ge.exhaust.service.ImageStorage;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final StoreRepository store;
    private final ImageStorage images;
    private static final List<String> CATEGORIES=List.of("Axle-back exhausts","Cat-back systems","Midpipes","Downpipes","Connecting pipes","Exhaust tips","Fitting hardware");
    public AdminController(StoreRepository store,ImageStorage images){this.store=store;this.images=images;}
    @ModelAttribute public void common(Model model){model.addAttribute("page","admin");model.addAttribute("adminCategories",CATEGORIES);}

    @GetMapping public String dashboard(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="all") String state,Model model) {
        var all=store.all(true);
        var products=store.find(new CatalogFilter(q,null,null,null,null,null,null,null),true).stream()
                .filter(p->switch(state){case "published"->p.published();case "hidden"->!p.published();case "out-of-stock"->!p.available();default->true;}).toList();
        model.addAttribute("products",products);model.addAttribute("q",q);model.addAttribute("state",state);
        model.addAttribute("total",all.size());model.addAttribute("visibleCount",all.stream().filter(Product::published).count());
        model.addAttribute("soldOutCount",all.stream().filter(p->!p.available()).count());
        model.addAttribute("adminPage","listings");return "admin/index";
    }
    @GetMapping("/listings/new") public String create(Model model) {
        model.addAttribute("form",new ListingForm());return editor(model,0,"/images/part-placeholder.svg");
    }
    @GetMapping("/listings/{id}/edit") public String edit(@PathVariable long id,Model model) {
        Product p=required(id);model.addAttribute("form",ListingForm.from(p));return editor(model,id,p.imagePath());
    }
    private String editor(Model model,long id,String image) {
        model.addAttribute("listingId",id);model.addAttribute("currentImage",image);model.addAttribute("adminPage","listings");return "admin/edit";
    }
    @PostMapping("/listings/save") public String save(@RequestParam(defaultValue="0") long id,
            @Valid @ModelAttribute("form") ListingForm form,BindingResult errors,
            @RequestParam(required=false) MultipartFile image,Model model,RedirectAttributes flash) {
        String current=id==0?"/images/part-placeholder.svg":required(id).imagePath();
        List<Fitment> fits=List.of();
        if(!errors.hasFieldErrors("compatibility")) {
            try{fits=form.parseFitments();}catch(IllegalArgumentException ex){errors.rejectValue("compatibility","invalid",ex.getMessage());}
        }
        if(!errors.hasFieldErrors("sku") && store.skuExists(form.getSku().strip(),id)) errors.rejectValue("sku","duplicate","This SKU already belongs to another listing.");
        if(!CATEGORIES.contains(form.getCategory())) errors.rejectValue("category","invalid","Choose a category from the list.");
        if(errors.hasErrors()) return editor(model,id,current);
        if(image!=null && !image.isEmpty()) {
            try{current=images.store(image);}catch(IllegalArgumentException ex){errors.reject("image",ex.getMessage());return editor(model,id,current);}
        }
        store.save(form.product(id,current,fits));flash.addFlashAttribute("success","Listing saved. The storefront is up to date.");return "redirect:/admin";
    }
    @PostMapping("/listings/{id}/visibility") public String visibility(@PathVariable long id,RedirectAttributes flash) {
        Product p=required(id);
        store.save(new Product(p.id(),p.sku(),p.title(),p.brand(),p.category(),p.description(),p.material(),p.priceCents(),p.imagePath(),p.sourceUrl(),p.imageCredit(),p.stockGoodwill(),p.stockGrmagele(),!p.published(),p.featured(),p.fitments()));
        flash.addFlashAttribute("success",p.published()?"Listing hidden from the storefront.":"Listing published on the storefront.");return "redirect:/admin";
    }
    @GetMapping("/listings/{id}/delete") public String confirmDelete(@PathVariable long id,Model model) {
        model.addAttribute("product",required(id));model.addAttribute("adminPage","listings");return "admin/delete";
    }
    @PostMapping("/listings/{id}/delete") public String delete(@PathVariable long id,RedirectAttributes flash) {
        required(id);store.delete(id);flash.addFlashAttribute("success","Listing deleted.");return "redirect:/admin";
    }
    @GetMapping("/landing") public String landing(Model model) {
        LandingForm form=new LandingForm();form.setTitle(store.setting("hero-title",""));form.setDescription(store.setting("hero-description",""));
        form.setCredit(store.setting("hero-credit",""));form.setSource(store.setting("hero-source",""));model.addAttribute("form",form);
        return landingEditor(model);
    }
    private String landingEditor(Model model) {
        model.addAttribute("heroImage",store.setting("hero-image","/images/bmw-g82.jpg"));
        model.addAttribute("adminPage","landing");return "admin/landing";
    }
    @PostMapping("/landing") public String saveLanding(@Valid @ModelAttribute("form") LandingForm form,BindingResult errors,
            @RequestParam(required=false) MultipartFile image,Model model,RedirectAttributes flash) {
        if(form.getTitle()!=null && form.getTitle().lines().count()>2) errors.rejectValue("title","invalid","Keep the headline to one or two lines.");
        if(errors.hasErrors()) return landingEditor(model);
        String uploaded=null;
        if(image!=null && !image.isEmpty()) {
            try{uploaded=images.store(image);}catch(IllegalArgumentException ex){errors.reject("image",ex.getMessage());return landingEditor(model);}
        }
        if(uploaded!=null) store.setSetting("hero-image",uploaded);
        store.setSetting("hero-title",form.getTitle().strip());store.setSetting("hero-description",form.getDescription().strip());
        store.setSetting("hero-credit",form.getCredit().strip());store.setSetting("hero-source",form.getSource().strip());
        flash.addFlashAttribute("success","Landing page saved.");return "redirect:/admin/landing";
    }
    private Product required(long id){return store.byId(id,true).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));}
}
