(function () {
  "use strict";

  /* ---- Mobile menu: close on outside click or Escape ---- */
  var mobileMenus = Array.prototype.slice.call(
    document.querySelectorAll(".mobile-site-menu")
  );

  if (mobileMenus.length) {
    document.addEventListener("click", function (event) {
      var target = event.target;
      if (!(target instanceof Element)) return;

      mobileMenus.forEach(function (menu) {
        if (!menu.open) return;
        var summary = target.closest(".mobile-site-menu > summary");
        var links = target.closest(".mobile-menu-links");
        if (summary || links) return;
        var panel = target.closest(".mobile-menu-panel");
        if (panel || !menu.contains(target)) {
          menu.open = false;
        }
      });
    });

    document.addEventListener("keydown", function (event) {
      if (event.key === "Escape") {
        mobileMenus.forEach(function (m) { m.open = false; });
      }
    });
  }

  /* ---- Lazy-load heavy images ---- */
  Array.prototype.forEach.call(
    document.querySelectorAll(
      ".screenshot-button img, .card-logo, .app-logo, .avatar"
    ),
    function (img) {
      img.setAttribute("loading", "lazy");
      img.setAttribute("decoding", "async");
    }
  );

  /* ---- Screenshot lightbox as a native <dialog> ---- */
  var buttons = Array.prototype.slice.call(
    document.querySelectorAll(".screenshot-button")
  );
  if (!buttons.length) return;

  var reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)");

  var dialog = document.createElement("dialog");
  dialog.className = "image-dialog";
  dialog.setAttribute("aria-label", "Expanded screenshot");
  dialog.setAttribute("tabindex", "-1");
  dialog.innerHTML =
    '<div class="image-dialog-inner">' +
    '  <img class="dialog-image" alt="">' +
    '  <p class="dialog-caption"></p>' +
    "</div>";
  document.body.appendChild(dialog);

  var dialogImage = dialog.querySelector(".dialog-image");
  var dialogCaption = dialog.querySelector(".dialog-caption");

  function captionFor(button) {
    var figure = button.closest("figure");
    var fig = figure && figure.querySelector("figcaption");
    if (fig && fig.textContent.trim()) return fig.textContent.trim();
    var img = button.querySelector("img");
    return img ? (img.getAttribute("alt") || "").trim() : "";
  }

  function srcFor(button) {
    var img = button.querySelector("img");
    return img ? img.getAttribute("src") || "" : "";
  }

  function updateDialog(button) {
    var caption = captionFor(button);
    dialogImage.src = srcFor(button);
    dialogImage.alt = caption || "Expanded screenshot";
    dialogCaption.textContent = caption;
  }

  var activeButton = null;
  var activeIndex = -1;
  var swapTimer = null;

  function syncDialogWidth() {
    if (!dialog.open) return;
    var w = dialogImage.getBoundingClientRect().width;
    if (w > 0) {
      dialog.style.setProperty("--dialog-media-width", Math.ceil(w) + "px");
    }
  }

  function withTransition(update) {
    if (!document.startViewTransition || reducedMotion.matches) {
      update();
      return Promise.resolve();
    }
    return document.startViewTransition(update).finished.catch(function () {});
  }

  function clearMorphNames() {
    if (activeButton) {
      var t = activeButton.querySelector("img");
      if (t) t.style.viewTransitionName = "";
    }
    dialogImage.style.viewTransitionName = "";
  }

  function openScreenshot(button) {
    activeButton = button;
    var thumb = button.querySelector("img");
    activeIndex = buttons.indexOf(button);
    if (thumb) thumb.style.viewTransitionName = "screenshot-morph";

    withTransition(function () {
      updateDialog(button);
      dialogImage.style.viewTransitionName = "screenshot-morph";
      if (thumb) thumb.style.viewTransitionName = "";
      dialog.showModal();
      syncDialogWidth();
    }).then(function () { dialog.focus(); });
  }

  function cycle(direction) {
    if (!dialog.open || !buttons.length) return;
    var next = (activeIndex + direction + buttons.length) % buttons.length;
    activeIndex = next;
    activeButton = buttons[next];
    var thumb = activeButton.querySelector("img");

    if (swapTimer) window.clearTimeout(swapTimer);
    if (reducedMotion.matches) {
      updateDialog(activeButton);
      return;
    }

    dialogImage.classList.add("is-switching");
    dialogCaption.classList.add("is-switching");
    swapTimer = window.setTimeout(function () {
      updateDialog(activeButton);
      window.requestAnimationFrame(function () {
        dialogImage.classList.remove("is-switching");
        dialogCaption.classList.remove("is-switching");
      });
      swapTimer = null;
    }, 140);
  }

  function closeScreenshot() {
    if (!dialog.open) return;
    if (swapTimer) {
      window.clearTimeout(swapTimer);
      swapTimer = null;
    }
    dialogImage.classList.remove("is-switching");
    dialogCaption.classList.remove("is-switching");

    withTransition(function () {
      var thumb = activeButton && activeButton.querySelector("img");
      if (thumb) thumb.style.viewTransitionName = "screenshot-morph";
      dialog.close();
      dialog.style.removeProperty("--dialog-media-width");
    }).then(function () {
      clearMorphNames();
      if (activeButton) activeButton.focus();
      activeButton = null;
      activeIndex = -1;
    });
  }

  buttons.forEach(function (button) {
    button.addEventListener("click", function () { openScreenshot(button); });
  });

  dialog.addEventListener("click", function (event) {
    if (event.target === this) closeScreenshot();
  });
  dialog.addEventListener("cancel", function (event) {
    event.preventDefault();
    closeScreenshot();
  });

  document.addEventListener("keydown", function (event) {
    if (!dialog.open) return;
    if (event.key === "ArrowRight") {
      event.preventDefault();
      cycle(1);
    } else if (event.key === "ArrowLeft") {
      event.preventDefault();
      cycle(-1);
    }
  });

  dialogImage.addEventListener("load", function () {
    window.requestAnimationFrame(syncDialogWidth);
  });
  window.addEventListener("resize", function () {
    window.requestAnimationFrame(syncDialogWidth);
  });
})();
