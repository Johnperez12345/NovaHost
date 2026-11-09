const express = require("express");
const helmet = require("helmet");
const multer = require("multer");
const AdmZip = require("adm-zip");
const slugify = require("slugify");
const rateLimit = require("express-rate-limit");
const fs = require("fs");
const path = require("path");
const crypto = require("crypto");

const app = express();
const PORT = process.env.PORT || 3000;
const API_KEY = process.env.ADMIN_API_KEY;
const DATA_DIR = process.env.DATA_DIR || path.join(__dirname, "data");
const SITES_DIR = path.join(DATA_DIR, "sites");
const MAX_ZIP_BYTES = 20 * 1024 * 1024;
const MAX_FILES = 500;
const MAX_UNPACKED_BYTES = 100 * 1024 * 1024;

fs.mkdirSync(SITES_DIR, { recursive: true });
app.use(helmet({ crossOriginResourcePolicy: { policy: "cross-origin" } }));
app.use(express.json({ limit: "32kb" }));
app.use(rateLimit({ windowMs: 60 * 1000, limit: 120, standardHeaders: true, legacyHeaders: false }));

const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: MAX_ZIP_BYTES, files: 1 }
});

function requireKey(req, res, next) {
  if (!API_KEY) return res.status(503).json({ error: "Server setup incomplete: ADMIN_API_KEY is not configured." });
  const supplied = (req.get("authorization") || "").replace(/^Bearer\s+/i, "");
  const a = Buffer.from(supplied);
  const b = Buffer.from(API_KEY);
  if (a.length !== b.length || !crypto.timingSafeEqual(a, b)) {
    return res.status(401).json({ error: "Unauthorized" });
  }
  next();
}

function safeSlug(value) {
  return slugify(String(value || ""), { lower: true, strict: true }).slice(0, 40);
}

app.get("/", (_req, res) => res.json({
  name: "NovaHost API",
  status: "online",
  message: "Static website hosting API is running.",
  endpoints: ["/health", "/api/sites", "/s/:slug/"]
}));
app.get("/health", (_req, res) => res.json({ ok: true }));

app.get("/api/sites", requireKey, (_req, res) => {
  const sites = fs.readdirSync(SITES_DIR, { withFileTypes: true })
    .filter(entry => entry.isDirectory())
    .map(entry => {
      const dir = path.join(SITES_DIR, entry.name);
      let bytes = 0;
      function walk(folder) {
        for (const item of fs.readdirSync(folder, { withFileTypes: true })) {
          const full = path.join(folder, item.name);
          if (item.isDirectory()) walk(full);
          else bytes += fs.statSync(full).size;
        }
      }
      walk(dir);
      return { slug: entry.name, url: `${req.protocol}://${req.get("host")}/s/${entry.name}/`, bytes };
    });
  res.json({ sites });
});

app.post("/api/sites", requireKey, upload.single("file"), (req, res) => {
  if (!req.file) return res.status(400).json({ error: "Attach a ZIP file using the 'file' field." });
  const slug = safeSlug(req.body.name || path.basename(req.file.originalname, ".zip"));
  if (!slug) return res.status(400).json({ error: "Please provide a valid site name." });
  const destination = path.join(SITES_DIR, slug);
  if (fs.existsSync(destination)) return res.status(409).json({ error: "That site name is already taken." });

  let zip;
  try { zip = new AdmZip(req.file.buffer); }
  catch (_err) { return res.status(400).json({ error: "Invalid ZIP archive." }); }

  const entries = zip.getEntries();
  if (!entries.length || entries.length > MAX_FILES) {
    return res.status(400).json({ error: `ZIP must contain 1-${MAX_FILES} files.` });
  }
  let unpacked = 0;
  for (const entry of entries) {
    const normalized = entry.entryName.replace(/\\/g, "/");
    if (normalized.startsWith("/") || normalized.split("/").some(part => part === "..") || normalized.includes("\0")) {
      return res.status(400).json({ error: "ZIP contains an unsafe file path." });
    }
    if (!entry.isDirectory) unpacked += entry.header.size || 0;
    if (unpacked > MAX_UNPACKED_BYTES) return res.status(400).json({ error: "Unpacked website exceeds 100 MB." });
  }

  try {
    fs.mkdirSync(destination, { recursive: false });
    zip.extractAllTo(destination, true);
    const indexPath = path.join(destination, "index.html");
    if (!fs.existsSync(indexPath) || !fs.statSync(indexPath).isFile()) {
      fs.rmSync(destination, { recursive: true, force: true });
      return res.status(400).json({ error: "ZIP must contain index.html at its root." });
    }
    res.status(201).json({
      message: "Website published.",
      slug,
      url: `${req.protocol}://${req.get("host")}/s/${slug}/`
    });
  } catch (err) {
    fs.rmSync(destination, { recursive: true, force: true });
    res.status(500).json({ error: "Could not publish website." });
  }
});

app.delete("/api/sites/:slug", requireKey, (req, res) => {
  const slug = safeSlug(req.params.slug);
  if (!slug) return res.status(400).json({ error: "Invalid site name." });
  const target = path.join(SITES_DIR, slug);
  if (!fs.existsSync(target)) return res.status(404).json({ error: "Site not found." });
  fs.rmSync(target, { recursive: true, force: true });
  res.json({ message: "Site deleted.", slug });
});

app.use("/s/:slug", (req, res, next) => {
  const slug = safeSlug(req.params.slug);
  if (!slug) return res.status(404).send("Site not found");
  const root = path.join(SITES_DIR, slug);
  if (!fs.existsSync(root)) return res.status(404).send("Site not found");
  express.static(root, { index: "index.html", fallthrough: false })(req, res, next);
});

app.use((err, _req, res, _next) => {
  if (err instanceof multer.MulterError) return res.status(400).json({ error: err.code === "LIMIT_FILE_SIZE" ? "ZIP exceeds 20 MB." : err.message });
  res.status(500).json({ error: "Unexpected server error." });
});

app.listen(PORT, "0.0.0.0", () => console.log(`NovaHost listening on ${PORT}`));