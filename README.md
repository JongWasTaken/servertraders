# ServerTraders
Adds server-controlled traders, which read their trades from config files and can update on the fly.

# Usage
Install the mod as usual.  
Run `/servertraders builder` to open the trader builder GUI.  
Set values as desired.  
Add trades with `/servertraders builder add-simple-offer [buy] [count] [sell] [count]` and  
`/servertraders builder add-full-offer [buy1] [count] [buy2] [count] [sell] [count]`.
Also set the villager appearance with `/servertraders builder set-appearance [biome] [profession]`.
Finally, confirm with `/servertraders builder`.

You can also edit the config files in `/config/servertraders/` directly.
Run `/servertraders reload` to apply changes.

Now you can place your trader with `/servertraders place`.

# License
MIT License