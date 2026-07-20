<div class="main-hero" style="display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 65vh; text-align: center; padding: 0 1rem; margin-top: -3rem;">
  <img src="assets/hapanels_logo_clean.svg" alt="Hapanels Logo" class="logo-img" style="width: 192px; height: 192px; margin-bottom: -1rem;">
  <h1 style="font-size: 2.5rem; font-weight: 700; margin: 0 0 1.2rem; padding: 0; color: var(--md-default-fg-color); letter-spacing: -0.03em;">Hapanels</h1>
  <p style="font-size: 0.9rem; font-weight: 300; max-width: 552px; line-height: 1.6; color: var(--md-default-fg-color); margin: 0 0 2.5rem; opacity: 0.85;">
    Aplikacja na ścienne panele dotykowe z systemem Android wraz z dedykowaną integracją do systemu Home Assistant.
    Oferuje bezpośrednią obsługę urządzeń Shelly Wall Display, integrując wbudowany przekaźnik, fizyczne przyciski oraz czujniki zbliżeniowe.
  </p>

  <!-- Separator sekcji -->
  <div style="width: 100%; max-width: 552px; border-top: 1px solid rgba(128, 128, 128, 0.15); margin: 1.5rem 0 2rem;"></div>

  <p style="font-size: 0.95rem; font-weight: 400; color: var(--md-default-fg-color); margin: 0 0 1.5rem; opacity: 0.9; text-align: center;">Projekt składa się z:</p>

  <!-- Architektura projektu (Dwukolumnowy Grid) - w pełni dynamiczny pod jasny i ciemny motyw -->
  <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 1.5rem; max-width: 700px; width: 100%; text-align: left; margin-bottom: 3.5rem;">
    <!-- Hapanels App -->
    <div style="border: 1px solid rgba(128, 128, 128, 0.15); border-radius: 12px; padding: 1.25rem; background: var(--md-code-bg-color); box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);">
      <h3 style="font-size: 1.05rem; font-weight: 600; margin: 0 0 0.5rem; color: var(--md-default-fg-color);">Hapanels</h3>
      <p style="font-size: 0.85rem; font-weight: 300; line-height: 1.55; color: var(--md-default-fg-color); opacity: 0.85; margin: 0;">
        Natywny klient na system Android, który wyświetla interfejs kafelkowy oraz obsługuje tryb uśpienia z energooszczędnym zegarem Always On Display.
      </p>
    </div>
    <!-- Hapanels Studio -->
    <div style="border: 1px solid rgba(128, 128, 128, 0.15); border-radius: 12px; padding: 1.25rem; background: var(--md-code-bg-color); box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);">
      <h3 style="font-size: 1.05rem; font-weight: 600; margin: 0 0 0.5rem; color: var(--md-default-fg-color);">Hapanels Studio</h3>
      <p style="font-size: 0.85rem; font-weight: 300; line-height: 1.55; color: var(--md-default-fg-color); opacity: 0.85; margin: 0;">
        Wizualny edytor zintegrowany z Home Assistant. Umożliwia wygodne projektowanie układu ekranu, edycję kafelków oraz natychmiastową synchronizację z panelami.
      </p>
    </div>
  </div>

  <!-- Nawigacja - Estetyczne przyciski konturowe -->
  <div style="display: flex; flex-wrap: wrap; justify-content: center; gap: 0.75rem; max-width: 750px; width: 100%; border-top: 1px solid rgba(128, 128, 128, 0.15); padding-top: 2rem;">
    <a href="installation/" style="display: inline-block; padding: 0.55rem 1.15rem; border: 1.5px solid #1E90FF; border-radius: 8px; font-size: 0.9rem; font-weight: 500; color: #1E90FF; text-decoration: none; background: rgba(30, 144, 255, 0.03); transition: background 0.2s, color 0.2s;">
      Instalacja
    </a>
    <a href="home-assistant-integration/" style="display: inline-block; padding: 0.55rem 1.15rem; border: 1.5px solid #1E90FF; border-radius: 8px; font-size: 0.9rem; font-weight: 500; color: #1E90FF; text-decoration: none; background: rgba(30, 144, 255, 0.03); transition: background 0.2s, color 0.2s;">
      Integracja Home Assistant
    </a>
    <a href="hardware/" style="display: inline-block; padding: 0.55rem 1.15rem; border: 1.5px solid #1E90FF; border-radius: 8px; font-size: 0.9rem; font-weight: 500; color: #1E90FF; text-decoration: none; background: rgba(30, 144, 255, 0.03); transition: background 0.2s, color 0.2s;">
      Sprzęt panelu
    </a>
    <a href="development/" style="display: inline-block; padding: 0.55rem 1.15rem; border: 1.5px solid #1E90FF; border-radius: 8px; font-size: 0.9rem; font-weight: 500; color: #1E90FF; text-decoration: none; background: rgba(30, 144, 255, 0.03); transition: background 0.2s, color 0.2s;">
      Development
    </a>
  </div>
</div>