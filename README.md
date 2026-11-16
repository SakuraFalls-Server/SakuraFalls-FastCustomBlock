# FastCustomBlock

Fastest* custom block helper library in the West.

## What is this?
This is a port of a helper library originally written in Denizen called `custom_block.dsc`. It is meant to
act as a complete replacement by providing the Denizen command `fastcustomblock` for adding or removing
a custom block at a location, and a `<fastcustomblock[<location[...]>]>` tag which provides the
`<[block].item>` and `<[block].location>` sub-tags.

It is version specific, and is kept up to date only with whatever server version we currently use.

## Why not pure Denizen?

Unfortunately, although a lot simpler to implement and robust enough for some time, it
has shown weaknesses in the way it relies on `fakespawn`. Although a very useful command,
Denizen does not expect it to be used for such insane behavior like what we attempted above.
This means that as blocks (especially player furniture in apartments...) would be used by players, 
the script we had used could not keep up with rendering a stress test and the TPS would drop
significantly, making it unreliable for the furniture update we had planned.

The new implementation was tested on a 75x50x75 dense cube area, which is a worst case scenario for the 
implementation we use, yielding 281,750 blocks in total. The server's TPS did not drop, except for JVM warmup 
and for huge disk reads, but we consider these acceptable for the time being. The worst case stress test 
scenario presented above is likely not going to happen any time soon.

## How to use

Just drop the compiled .jar onto your server. By itself, this does nothing. To use it, you will either need 
to use a Java plugin which hooks into its API, see `FastCustomBlocksMain.getCustomBlockService()` for the right 
commands, or you will need a Denizen script which uses the command:
```dsc
- fastcustomblock [<location>] [<item>] [cancel]
- define block <fastcustomblock[locationTag...]>
```

Unfortunately, the syntax highlighting will have issues with Denizen, so we recommend to also use the helper
`custom_block.dsc` library found in the 
[SakuraFalls-Denizen-Scripts](https://github.com/SakuraFalls-Server/SakuraFalls-Denizen-Scripts/blob/main/scripts/lib/custom_block.dsc) 
repository.

---

*\* We don't actually know if it is the fastest, but it is pretty fast!*