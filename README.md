# Epsilon Playground Back-End

This repo hosts a copy of the back-end code of the [Epsilon Playground](https://www.eclipse.org/epsilon/live/). The source code for its front-end is in [Epsilon's website repo](https://git.eclipse.org/c/www.eclipse.org/epsilon.git/tree/mkdocs/docs/live).

To run the Epsilon Playground front-end, follow the instructions provided [here](https://www.eclipse.org/epsilon/doc/articles/manage-the-epsilon-website-locally/) to clone and run Epsilon's website. 

To run the back-end locally too, you should clone this repo, run its `pom.xml` and replace the relevant `gcp.cloudfunctions.net` URLs in the front-end's `index.html` with `localhost:8080` to make the front-end call the local instances of the respective web services.

