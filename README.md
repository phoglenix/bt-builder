# bt-builder

This is prototype code for building a behaviour tree from examples of expert behaviour.
This code is explained in the accompanying paper [Building Behavior Trees from Observations in Real-Time Strategy Games](https://www.cs.auckland.ac.nz/research/gameai/publications.php).

To run, this code requires:
 - [MEME Suite v4.10.0_4](http://meme-suite.org/doc/download.html) modified to allow for larger alphabets (modifications included here).
 - [XStream v1.4.7](https://x-stream.github.io/) for serialisation of trees.
 - Apache Commons IO and Collections.

As it is currently set up to work with Starcraft data, it also requires:
 - Starcraft gameplay traces as [recorded](http://scidrive.uoa.auckland.ac.nz/gameai/) by [ScExtractor](https://github.com/phoglenix/ScExtractor).
 - A JDBC driver to read the Starcraft databases.
 - [JNIBWAPI v1.0](https://github.com/JNIBWAPI/JNIBWAPI/releases/tag/v1.0) for Starcraft data handling (included here).

## Disclaimer

This is very much [research code](http://www.phdcomics.com/comics/archive.php?comicid=961). No guarantees are provided that this code will/won't do anything at all. Use at own risk.

## License

My code is licensed under the MIT license. However, this project relies on modified code from the [MEME Suite](http://meme-suite.org/) which has a separate license. The modified parts of MEME code and MEME license are available in the meme_4.10.0 directory.
