# OpenGeophysicalModelLibrary-java

A library of geophysical reference models for simulation, navigation, and sensor fusion.

Pure Java, with a small dependency on [OpenNumericalLibrary-java](https://github.com/pbernalpolo/OpenNumericalLibrary-java)
(linear algebra and special functions). Each model is a plain class with a focused API.

## Models

| Package | Key types | What it provides |
|---------|-----------|------------------|
| `magnetic` | `WorldMagneticModel` | The World Magnetic Model (WMM): Earth's main (core) magnetic field (in SI tesla) from spherical-harmonic Gauss coefficients (a `.COF` file, in nT), including secular variation evaluated at a decimal year. |
| `gravity` | `SphericalHarmonicGravityModel`, `Egm2008`, `Egm96` | Gravitational potential and gravity vector from spherical-harmonic coefficients (a `.gfc` file), plus geoid undulation / height anomaly. `Egm2008` and `Egm96` are the EGM2008 / EGM96 instances. |
| `atmosphere` | `AtmosphericModel`, `InternationalStandardAtmosphere` | The ISA / U.S. Standard Atmosphere 1976 (seven layers): temperature, pressure, density, speed of sound, and their altitude derivatives. Each layer is itself a usable model. |
| `terrain` | `TerrainElevationModel`, `GriddedTerrainElevationModel` | Terrain elevation as a function of latitude/longitude, backed by a gridded raster (ESRI ASCII Grid `.asc` or GeoTIFF `.tif`) with bilinear interpolation. |
| `referenceSystems` | `GeographicCoordinates`, `GeodeticCoordinates`, `GeocentricCoordinates`, `Wgs84`, `Grs80`, … | Geodetic/geocentric coordinate representations and conversions, and the WGS84 / GRS80 reference ellipsoids. |

## Conventions

- **Field models** (magnetic, gravity) are evaluated at a 3-D position in the Earth-fixed Cartesian (ECEF) frame, in
  metres: `setPosition(Vector3)` then read the result. Outputs are returned in the same Earth-fixed frame.
- **The elevation model** is a 2-D surface query: `elevationAt(latitudeRadians, longitudeRadians)` returning metres,
  `NaN` where there is no data. **Angles are in radians.**
- **The atmosphere** is set by geometric altitude above mean sea level, in metres.
- Exact frames, units, and datums are documented in each class's Javadoc.

## Dependencies

`OpenNumericalLibrary-java` is included as a git submodule under `lib/`. Clone recursively (or initialize it
afterwards):

```bash
git clone --recurse-submodules <repo-url>
# or, in an existing clone:
git submodule update --init
```

## Building and testing

Standard Maven/Eclipse source layout (`src/main/java`, `src/test/java`). The code uses Java 9+ language features
(private interface methods); it builds and runs on **JDK 17**. Tests are **JUnit 5** and run from your IDE.

Some tests load data files (for example `WMM.COF` for the magnetic tests, `EGM2008.gfc` for the gravity tests). These
are **not committed** here (they are large and/or carry their own third-party licenses). Each consumer looks for its
file at `res/<package>/…` **relative to the working directory** — for example `res/magnetic/WMM2025COF/WMM.COF` and
`res/gravity/EGM2008.gfc`. Place the files there and run from the project root to exercise those tests; when a file is
absent the corresponding tests are skipped (not failed).

## License

Licensed under the **MIT License** — see [`LICENSE`](LICENSE). It is original code with no third-party copyleft
bundled. Data files loaded at runtime carry their providers' own terms.
