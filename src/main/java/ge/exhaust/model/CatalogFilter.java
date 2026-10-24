package ge.exhaust.model;

public record CatalogFilter(String q, String make, String model, Integer year,
                            String category, String storage, String availability, String sort) {
    public CatalogFilter {
        q = clean(q); make = clean(make); model = clean(model); category = clean(category);
        storage = clean(storage); availability = clean(availability); sort = clean(sort);
    }
    private static String clean(String value) { return value == null ? "" : value.strip(); }
    public boolean active() {
        return !q.isEmpty() || !make.isEmpty() || !model.isEmpty() || year != null
                || !category.isEmpty() || !storage.isEmpty() || !availability.isEmpty();
    }
}
