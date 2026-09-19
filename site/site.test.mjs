import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const html = await readFile(new URL("index.html", import.meta.url), "utf8");
const css = await readFile(new URL("styles.css", import.meta.url), "utf8");
const script = await readFile(new URL("site.js", import.meta.url), "utf8");

test("the tour contains the core product and architecture sections", () => {
  for (const id of [
    "product",
    "journeys",
    "architecture",
    "reliability",
    "run-locally",
  ]) {
    assert.match(html, new RegExp(`id="${id}"`));
  }
});

test("links and assets remain portable on a GitHub Pages subpath", () => {
  assert.doesNotMatch(html, /(?:href|src)="\/(?!\/)/);
  assert.match(html, /href="https:\/\/github\.com\/wiznick79\/hotel-booking"/);
  assert.match(html, /src="assets\/hotel-hero\.png"/);
});

test("interactive architecture controls are accessible", () => {
  assert.match(html, /aria-controls="service-detail"/);
  assert.match(html, /aria-live="polite"/);
  assert.match(script, /addEventListener\("click"/);
});

test("responsive and reduced-motion styles are present", () => {
  assert.match(css, /@media \(max-width: 760px\)/);
  assert.match(css, /prefers-reduced-motion: reduce/);
});
