package ge.exhaust.model;

public record Fitment(String make, String model, int yearFrom, int yearTo, String engine) {
    public String label() { return make + " " + model; }
    public String years() { return yearFrom + "–" + yearTo; }
}
