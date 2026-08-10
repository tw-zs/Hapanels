<div class="main-hero" style="display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 65vh; text-align: center; padding: 0 1rem; margin-top: -3rem;">
  <img src="../assets/hapanels_logo_clean.svg" alt="Hapanels Logo" class="logo-img" style="width: 192px; height: 192px; margin-bottom: -1rem;">
  <h1 style="font-size: 2.5rem; font-weight: 700; margin: 0 0 1.2rem; padding: 0; color: var(--md-default-fg-color); letter-spacing: -0.03em;">Hapanels</h1>
  <p style="font-size: 0.9rem; font-weight: 300; max-width: 552px; line-height: 1.6; color: var(--md-default-fg-color); margin: 0 0 2.5rem; opacity: 0.85;">
    An Android application for wall-mounted touch panels with a dedicated Home Assistant integration.
    Offers direct support for Shelly Wall Display devices, integrating the built-in relay, physical buttons, and proximity sensors.
  </p>

  <img src="../assets/screenshots/hapanels-en-hero.png" alt="Hapanels Grid home dashboard" style="width: 100%; max-width: 920px; border-radius: 16px; box-shadow: 0 18px 50px rgba(0, 0, 0, 0.28);">

  <!-- Section separator -->
  <div style="width: 100%; max-width: 552px; border-top: 1px solid rgba(128, 128, 128, 0.15); margin: 1.5rem 0 2rem;"></div>

  <p style="font-size: 0.95rem; font-weight: 400; color: var(--md-default-fg-color); margin: 0 0 1.5rem; opacity: 0.9; text-align: center;">The project consists of:</p>

  <!-- Project Architecture (Two-column Grid) -->
  <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 1.5rem; max-width: 700px; width: 100%; text-align: left; margin-bottom: 3.5rem;">
    <!-- Hapanels App -->
    <div style="border: 1px solid rgba(128, 128, 128, 0.15); border-radius: 12px; padding: 1.25rem; background: var(--md-code-bg-color); box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);">
      <h3 style="font-size: 1.05rem; font-weight: 600; margin: 0 0 0.5rem; color: var(--md-default-fg-color);">Hapanels</h3>
      <p style="font-size: 0.85rem; font-weight: 300; line-height: 1.55; color: var(--md-default-fg-color); opacity: 0.85; margin: 0;">
        Native Android client displaying a tile interface and supporting sleep mode with an energy-efficient Always On Display clock.
      </p>
    </div>
    <!-- Hapanels Studio -->
    <div style="border: 1px solid rgba(128, 128, 128, 0.15); border-radius: 12px; padding: 1.25rem; background: var(--md-code-bg-color); box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);">
      <h3 style="font-size: 1.05rem; font-weight: 600; margin: 0 0 0.5rem; color: var(--md-default-fg-color);">Hapanels Studio</h3>
      <p style="font-size: 0.85rem; font-weight: 300; line-height: 1.55; color: var(--md-default-fg-color); opacity: 0.85; margin: 0;">
        Visual editor integrated with Home Assistant. Enables convenient screen layout design, tile editing, and instant synchronization with panels.
      </p>
    </div>
    <div style="border: 1px solid rgba(30, 144, 255, 0.4); border-radius: 12px; padding: 1.25rem; background: var(--md-code-bg-color); box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);">
      <h3 style="font-size: 1.05rem; font-weight: 600; margin: 0 0 0.5rem; color: var(--md-default-fg-color);">Easy Install</h3>
      <p style="font-size: 0.85rem; font-weight: 300; line-height: 1.55; color: var(--md-default-fg-color); opacity: 0.85; margin: 0 0 0.75rem;">
        Use the ready-made AI agent prompt to install the APK on Shelly through ADB and deploy Studio to Home Assistant.
      </p>
      <a href="https://github.com/tw-zs/Hapanels#install-with-an-ai-agent" style="font-size: 0.85rem; font-weight: 500; color: #1E90FF; text-decoration: none;">Open the guide →</a>
    </div>
  </div>

  <img src="../assets/screenshots/studio-en-layout.png" alt="Hapanels Studio layout editor" style="width: 100%; max-width: 920px; border-radius: 16px; box-shadow: 0 18px 50px rgba(0, 0, 0, 0.28); margin-bottom: 1rem;">
  <p style="font-size: 0.85rem; font-weight: 300; max-width: 700px; line-height: 1.55; color: var(--md-default-fg-color); margin: 0 0 2.5rem; opacity: 0.85;">Hapanels Studio lets you arrange tiles, manage the side tray, and save changes directly to Home Assistant.</p>

  <!-- Navigation buttons -->
  <div style="display: flex; flex-wrap: wrap; justify-content: center; gap: 0.75rem; max-width: 750px; width: 100%; border-top: 1px solid rgba(128, 128, 128, 0.15); padding-top: 2rem;">
    <a href="installation/" style="display: inline-block; padding: 0.55rem 1.15rem; border: 1.5px solid #1E90FF; border-radius: 8px; font-size: 0.9rem; font-weight: 500; color: #1E90FF; text-decoration: none; background: rgba(30, 144, 255, 0.03); transition: background 0.2s, color 0.2s;">
      Installation
    </a>
    <a href="home-assistant-integration/" style="display: inline-block; padding: 0.55rem 1.15rem; border: 1.5px solid #1E90FF; border-radius: 8px; font-size: 0.9rem; font-weight: 500; color: #1E90FF; text-decoration: none; background: rgba(30, 144, 255, 0.03); transition: background 0.2s, color 0.2s;">
      Home Assistant Integration
    </a>
    <a href="hardware/" style="display: inline-block; padding: 0.55rem 1.15rem; border: 1.5px solid #1E90FF; border-radius: 8px; font-size: 0.9rem; font-weight: 500; color: #1E90FF; text-decoration: none; background: rgba(30, 144, 255, 0.03); transition: background 0.2s, color 0.2s;">
      Panel Hardware
    </a>
    <a href="development/" style="display: inline-block; padding: 0.55rem 1.15rem; border: 1.5px solid #1E90FF; border-radius: 8px; font-size: 0.9rem; font-weight: 500; color: #1E90FF; text-decoration: none; background: rgba(30, 144, 255, 0.03); transition: background 0.2s, color 0.2s;">
      Development
    </a>
  </div>
</div>
