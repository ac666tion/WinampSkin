(function () {
  "use strict";

  var VIEWPORT = "width=device-width, initial-scale=1.0";
  var STYLE_ID = "lamaniec-legacy-responsive-fix";

  function applyViewport() {
    var root = document.documentElement;
    if (!root) return;

    var head = document.head || root;
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
        "}";
      head.appendChild(style);
    }
  }

  applyViewport();

  if (window.MutationObserver && document.documentElement) {
    var observer = new MutationObserver(function () {
      applyViewport();
    });
    observer.observe(document.documentElement, {
      childList: true,
      subtree: true,
      attributes: true,
      attributeFilter: ["content"]
    });
  }

  document.addEventListener("DOMContentLoaded", applyViewport, false);
})();
