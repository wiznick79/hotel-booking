const serviceDetails = {
  gateway: {
    kicker: "Edge / request protection",
    title: "API Gateway",
    body:
      "Routes every client request, propagates correlation IDs, applies " +
      "Redis-backed rate limits, and isolates downstream failures with timeouts and circuit breakers.",
    tags: ["Spring Cloud Gateway", "Redis", "Resilience4j"],
  },
  identity: {
    kicker: "Identity boundary",
    title: "Identity Service",
    body:
      "Owns staff and customer identities, roles, permissions, hotel assignments, " +
      "email verification, refresh tokens, and RS256 access-token issuance.",
    tags: ["JWT / RS256", "RBAC", "Transactional outbox"],
  },
  hotel: {
    kicker: "Inventory boundary",
    title: "Hotel Service",
    body:
      "Owns hotels, translated room types, physical rooms, rates, booking policies, " +
      "dated availability blocks, and configurable website media.",
    tags: ["Inventory", "Pricing", "Media storage"],
  },
  booking: {
    kicker: "Reservation boundary",
    title: "Booking Service",
    body:
      "Revalidates availability, freezes price snapshots, assigns physical rooms, " +
      "manages reservation state, initiates Stripe Checkout, and publishes domain events.",
    tags: ["Reservations", "Stripe", "Outbox + Kafka"],
  },
  notification: {
    kicker: "Communication boundary",
    title: "Notification Service",
    body:
      "Consumes reservation and identity events, records delivery state, sends email " +
      "through a provider abstraction, and keeps failures available for controlled retry.",
    tags: ["Kafka consumer", "SMTP", "Dead-letter topics"],
  },
};

const detail = document.querySelector("#service-detail");
const serviceButtons = document.querySelectorAll("[data-service]");

function selectService(button) {
  const service = serviceDetails[button.dataset.service];
  if (!service || !detail) return;

  serviceButtons.forEach((candidate) => {
    const selected = candidate === button;
    candidate.classList.toggle("selected", selected);
    candidate.setAttribute("aria-pressed", String(selected));
  });

  detail.innerHTML = `
    <p class="eyebrow">${service.kicker}</p>
    <h3>${service.title}</h3>
    <p>${service.body}</p>
    <ul class="tag-list">${service.tags.map((tag) => `<li>${tag}</li>`).join("")}</ul>
  `;
}

serviceButtons.forEach((button) => {
  button.addEventListener("click", () => selectService(button));
});

const menuButton = document.querySelector(".menu-button");
const navigation = document.querySelector("#site-navigation");

menuButton?.addEventListener("click", () => {
  const open = menuButton.getAttribute("aria-expanded") === "true";
  menuButton.setAttribute("aria-expanded", String(!open));
  navigation?.classList.toggle("open", !open);
});

navigation?.addEventListener("click", (event) => {
  if (!(event.target instanceof HTMLAnchorElement)) return;
  menuButton?.setAttribute("aria-expanded", "false");
  navigation.classList.remove("open");
});
