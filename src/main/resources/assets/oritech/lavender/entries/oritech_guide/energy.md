```json
{
  "title": "Energy",
  "icon": "oritech:energy_pipe",
  "category": "oritech:logistics",
  "associated_items": [
    "oritech:energy_pipe",
    "oritech:small_storage_block",
    "oritech:large_storage_block"
  ]
}
```

Oritech uses **RF** to power all of its machines. It uses the Reborn Energy API to do so. This means Oritech is compatible with
all mods using the Tech Reborn energy system, which currently includes basically all fabric energy using mods.

There is only 1 cable tier available, capable of transferring up to 10k RF/t.

;;;;;

Generators will always output energy, and all other machines accept
energy from all sides (and won't output it again). The cables themselves store up to 10k RF in each machine connection, if it can't output the energy.

;;;;;

To buffer and store energy, you can use energy storage blocks. They're available in 2 sizes, and can be massively expanded using addons.
Energy storage blocks accept energy from all sides with a {green}green port{}, and can only output to the single {red}red port{}.

<block;oritech:small_storage_block>

;;;;;

Energy can also be transferred wireless using an enderic laser. For more information, see page [enderic laser](^oritech:enderic_laser)

<block;oritech:laser_arm_block>
