# Epsilon Playground Back-End

This repo hosts a copy of the back-end code of the [Epsilon Playground](https://www.eclipse.org/epsilon/live/). The source code for its front-end is in [Epsilon's website repo](https://git.eclipse.org/c/www.eclipse.org/epsilon.git/tree/mkdocs/docs/live).

To run the Epsilon Playground front-end, follow the instructions provided [here](https://www.eclipse.org/epsilon/doc/articles/manage-the-epsilon-website-locally/) to clone and run Epsilon's website. 

To run the back-end locally too, you should clone this repo, run the following commands 

- `mvn function:run -Drun.functionTarget=org.eclipse.epsilon.live.RunEpsilonFunction -Drun.port=8001`
- `mvn function:run -Drun.functionTarget=org.eclipse.epsilon.live.FlexmiToPlantUMLFunction -Drun.port=8002`
- `mvn function:run -Drun.functionTarget=org.eclipse.epsilon.live.EmfaticToPlantUMLFunction -Drun.port=8003`

and update the frontend's `backend.json` as follows to make the front-end call the local instances of the respective web services.

```json
{
  "service": [
    {"name": "RunEpsilonFunction", "port": "8001"},
    {"name": "FlexmiToPlantUMLFunction", "port": "8002"},
    {"name": "EmfaticToPlantUMLFunction", "port": "8003"},
    {"name": "ShortURLFunction", "url": "https://europe-west2-epsilon-live-gcp.cloudfunctions.net/short-url"}
  ]
}
```