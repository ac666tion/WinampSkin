(function () {
  "use strict";

  var VIEWPORT = "width=device-width, initial-scale=1.0";
  var STYLE_ID = "lamaniec-legacy-responsive-fix";

  function applyViewport() {
    var root = document.documentElement;
    var head = document.head;
    if (!root || !head) return;

    var meta = document.querySelector('meta[name="viewport"]');
    if (!meta) {
      meta = document.createElement("meta");
      meta.setAttribute("name", "viewport");
      head.insertBefore(meta, head.firstChild || null);
    }

    if (meta.getAttribute("content") !== VIEWPORT) {
      meta.setAttribute("content", VIEWPORT);
    }

    if (!document.getElementById(STYLE_ID)) {
      var style = document.createElement("style");
      style.id = STYLE_ID;
      style.textContent =
        "html, body {" +
        "width: 100% !important;" +
        "min-width: 0 !important;" +
        "max-width: 100% !important;" +
        "}" +
        "body {" +
        "margin-left: auto !important;" +
        "margin-right: auto !important;" +
        "}" +
        "img, svg, video, canvas {" +
        "max-width: 100% !important;" +
        "}" +
        "p, h1, h2, h3, h4, li, label, small {" +
        "white-space: normal !important;" +
        "overflow-wrap: break-word;" +
        "word-wrap: break-word;" +
        "}" +
        "@media (max-width: 700px) {" +
        "main, #root, #__next {" +
        "max-width: 100% !important;" +
        "min-width: 0 !important;" +
        "}" +
        "}" +
        /* Compatibility test: disable CSS motion and card flip transitions. */
        "*, *::before, *::after {" +
        "animation-duration: 1ms !important;" +
        "animation-delay: 0ms !important;" +
        "animation-iteration-count: 1 !important;" +
        "transition-duration: 1ms !important;" +
        "transition-delay: 0ms !important;" +
        "scroll-behavior: auto !important;" +
        "}" +
        /* Prevent decorative pseudo-elements from intercepting card taps. */
        "::before, ::after {" +
        "pointer-events: none !important;" +
        "}" +
        "* {" +
        "-webkit-tap-highlight-color: transparent;" +
        "}";
      head.appendChild(style);
    }
  }

  function stopWebAnimations() {
    try {
      if (document.getAnimations) {
        var animations = document.getAnimations();
        for (var i = 0; i < animations.length; i++) {
          try { animations[i].finish(); } catch (ignore) {}
        }
      }
    } catch (ignore) {}
  }

  if (window.MutationObserver) {
    var observer = new MutationObserver(function () {
      applyViewport();
      stopWebAnimations();
    });
    observer.observe(document, {
      childList: true,
      subtree: true,
      attributes: true,
      attributeFilter: ["content", "class", "style"]
    });
  }

  applyViewport();
  stopWebAnimations();
  document.addEventListener("DOMContentLoaded", function () {
    applyViewport();
    stopWebAnimations();
  }, false);
})();
