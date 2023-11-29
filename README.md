# Statify - Simple Minecraft Stats Done Right

Hi! I'm @quantumbagel, and this is my first plugin - Statify. It's a tiny (<50kb), resource-efficient, and powerful serverside plugin to get stats for one player - or the entire server.

### Why do I need this?

If you want a way to aggregate stats throughout the server (not just through the Statistics menu for one player - this plugin allows easy visualization of stats.


### Pros/Cons

Here's a list of pros and cons of this plugin (this isn't a *perfect* plugin)

#### Pros

* Complex queries allowed (`killed:zombie+killed:skeleton`, `(killed:zombie+killed:skeleton)^custom:beds_broken`, or even `tan(killed:zombie) + killed:skeleton`)
* Ranking of individual categories for a certain player
* Ability to create and save complex queries under a shorthand
* Yes, there *is* a recursion detector

#### Cons
* No autocomplete, you have to know the general layout of MC stats (for an excellent guide, check https://minecraft.fandom.com/wiki/Statistics)
* My first plugin, so there are probably copious quantities of bugs and glitches
* Doesn't work well with offline-mode (cracked Minecraft) because of the possibility of duplicate usernames
* No multithreading
* No custom command sanity checks
* Bugginess with really big numbers >:(
This is a *Bukkit* plugin, so put the .jar in the `plugins` folder of your Spigot/Bukkit/CraftBukkit server. 


### How do I even use these commands?
*NB: [description] is NOT REQUIRED, equals sign after is DEFAULT VALUE, <description> IS REQUIRED*
### /leaderboard, /lb


#### Format:


`/leaderboard [number of top entries]=10 <complex expression>`


#### Examples:


`/lb killed:zombie` (A simple request for the top 10 player zombie kills)


`/lb 5 killed:zombie` (A request for the top 5 player zombie kills)




`/lb killed:total` (A request for the player with the highest sum of the "killed" category (most entity kills))


`/lb APlayerOfMinecraft:kd` (APlayerOfMinecraft is username, kd is custom command that user defined (in this case kill differential))


`/lb killed:zombie + APlayerOfMinecraft:kd` (You can use both in conjunction using these stats as numbers)


`/lb killed:zombie * APlayerOfMinecraft:kd + custom:walk_one_cm`

### /stat
#### Format:


`/stat [username]=<player who is sending> <category> [number of top entries]=10`


#### Examples


`/stat killed` (returns top 10 ranking of the "killed" category for the player who sent the message)


`/stat killed 5` (returns top 10 ranking of the "killed" category for the player who sent the message)


`/stat APlayerOfMinecraft killed` (returns top 10 ranking of the "killed" category of the player "APlayerOfMinecraft")


`/stat APlayerOfMinecraft killed 5` (returns top 5 ranking of the "killed" category of the player "APlayerOfMinecraft")


### /custom


#### Format:


`/custom set <command name> <complex expression>`


`/custom list [username]=<player who is sending>`


`/custom get [username]=<player who is sending> <command name>`


`/custom delete <command name>`


#### Examples


`/custom set kd killed:total-killed_by:total` (Set the player who is sending's custom command "kd" to "killed:total-killed_by:total". If the player's name was "APlayerOfMinecraft," this could be referenced the exact same way as the `/leaderboard` examples above.)


`/custom list` (List the current player's defined commands)


`/custom list APlayerOfMinecraft` (List the player "APlayerOfMinecraft"'s defined stats)


`/custom get kd` (Print the current definition of the custom stat `kd` for the current player)


`/custom get APlayerOfMinecraft kd` (Print the current definition of the custom stat `kd` for the player "APlayerOfMinecraft")


`/custom delete kd` (Delete the command `kd` for the current player, if defined)


### /playerrank


#### Format


`/playerrank [number of top entries]=10`


#### Examples


`/playerrank` (List the top 10 players on the server using the PlayerScore algorithm that I made up on the spot)


`/playerrank 5` (List the top 5 players on the server)


That's all - I hope you enjoy this little plugin!
