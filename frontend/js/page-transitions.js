(() => {
  const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)");

  if (reducedMotion.matches) {
    return;
  }

  document.body.classList.add("page-transition-enter");

  document.addEventListener("click", (event) => {
    const target = event.target;

    if (!(target instanceof Element)) {
      return;
    }

    const link = target.closest("a[data-page-transition]");

    if (
      !(link instanceof HTMLAnchorElement) ||
      event.defaultPrevented ||
      event.button !== 0 ||
      event.metaKey ||
      event.ctrlKey ||
      event.shiftKey ||
      event.altKey ||
      (link.target && link.target !== "_self") ||
      link.hasAttribute("download")
    ) {
      return;
    }

    const destination = new URL(link.href);

    if (
      destination.origin !== window.location.origin ||
      (destination.pathname === window.location.pathname &&
        destination.search === window.location.search)
    ) {
      return;
    }

    event.preventDefault();
    document.body.classList.remove("page-transition-enter");
    document.body.classList.add("page-transition-exit");

    window.setTimeout(() => {
      window.location.assign(destination.href);
    }, 150);
  });
})();
