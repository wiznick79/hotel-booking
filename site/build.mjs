import { copyFile, mkdir, rm } from "node:fs/promises";
import { fileURLToPath } from "node:url";

const output = new URL("../target/project-site/", import.meta.url);
const assets = new URL("assets/", output);

await rm(output, { recursive: true, force: true });
await mkdir(assets, { recursive: true });

for (const filename of ["index.html", "styles.css", "site.js"]) {
  await copyFile(new URL(filename, import.meta.url), new URL(filename, output));
}

const imageSource = new URL(
  "../frontend/public-web/src/assets/",
  import.meta.url,
);
const images = {
  "hotel-hero.png": "hotel-hero.png",
  "hotel-room.png": "hotel-room-gallery.png",
  "hotel-suite.png": "hotel-suite-gallery.png",
  "hotel-exterior.png": "hotel-exterior-gallery.png",
};

for (const [target, source] of Object.entries(images)) {
  await copyFile(new URL(source, imageSource), new URL(target, assets));
}

console.log(`Static project site built at ${fileURLToPath(output)}`);
