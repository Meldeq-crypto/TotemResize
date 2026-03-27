Since you're specifically targeting 1.21.4, your README should highlight the fact that this version is optimized for the major internal changes Mojang made to item models in that specific update.

Here is a clean, professional README designed for the 1.21.4 branch of your GitHub.

Totem Pop (Scale) | v1.21.4
This branch contains the optimized version of Totem Pop specifically for Minecraft 1.21.4. This version leverages the new internal rendering hooks to provide the smoothest scaling experience yet.

🚀 Key Features (v1.21.4)
Hybrid Configuration: Choose between a Live Drag system (click and move the totem on your screen) or Precision Sliders for exact scaling.

1.21.4 Model Support: Fully compatible with the 1.21.4 item model registry—works perfectly with high-poly or 3D resource pack totems.

Independent Pop Scaling: A dedicated slider allows you to change the size of the "Pop" animation without affecting how the totem looks in your hand.

Static Mode: New toggle to disable the vanilla "bobbing" or swaying animation, keeping the totem locked in your preferred position.

🛠 Installation
Ensure you are running Minecraft 1.21.4.

Install the latest Fabric Loader.

Download the .jar for this version and place it in your mods folder.

Required Dependencies: * 

 (Required for the menu)

⚙️ Configuration
Open the in-game menu (via Mod Menu) or edit the config file:
config/totempop-1.21.4.json

🐞 Known Issues & Fixes
Offhand Rendering: The common "mirroring" glitch where one side of the totem disappeared in the offhand has been fully resolved in this version.

Z-Fighting: Optimized depth-testing ensures the totem doesn't flicker when scaled to very small sizes.
