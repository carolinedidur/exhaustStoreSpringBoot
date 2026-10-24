// Native forms work without JavaScript. This only improves a few controls.
document.querySelectorAll("[data-vehicle-form]").forEach((form) => {
  const make = form.querySelector("[data-make]");
  const model = form.querySelector("[data-model]");
  if (!make || !model) return;
  const options = [...model.querySelectorAll("option[data-make]")].map(
    (option) => ({
      make: option.dataset.make,
      value: option.value,
      label: option.textContent,
    }),
  );
  function updateModels(clear) {
    const selected = clear ? "" : model.value;
    model.replaceChildren(new Option("All models", ""));
    options
      .filter((option) => !make.value || option.make === make.value)
      .forEach((option) => {
        model.add(
          new Option(
            option.label,
            option.value,
            false,
            option.value === selected,
          ),
        );
      });
  }
  make.addEventListener("change", () => updateModels(true));
  updateModels(false);
});
document.querySelectorAll("[data-auto-submit]").forEach((select) => {
  select.addEventListener("change", () => select.form.requestSubmit());
});
const filters = document.querySelector(".filter-panel");
if (filters) {
  const media = window.matchMedia("(max-width: 700px)");
  const sync = () => {
    filters.open = !media.matches;
  };
  media.addEventListener("change", sync);
  sync();
}
document.querySelectorAll("[data-image-input]").forEach((input) => {
  let objectUrl;
  input.addEventListener("change", () => {
    const file = input.files[0];
    const preview = input.form.querySelector("[data-image-preview]");
    if (!file || !preview) return;
    if (objectUrl) URL.revokeObjectURL(objectUrl);
    objectUrl = URL.createObjectURL(file);
    preview.src = objectUrl;
  });
});
