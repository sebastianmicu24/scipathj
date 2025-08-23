# H&E Color Deconvolution

This document explains the H&E (Hematoxylin & Eosin) color deconvolution implementation in SciPathJ.

## Overview

Color deconvolution is a technique used to separate stains in histological images. For H&E stained slides, this process extracts three channels:
- **Hematoxylin channel**: Shows nuclei and other basophilic structures (blue/purple)
- **Eosin channel**: Shows cytoplasmic and extracellular structures (pink/red)
- **Background channel**: Shows the complement of the other two channels

## Algorithm

The implementation uses the **Ruifrok & Johnston method** for color deconvolution, which is the same algorithm used by Fiji's Color Deconvolution plugin.

### Mathematical Process

1. **RGB to Optical Density Conversion**:
   ```
   OD = -log10(RGB / 255.0)
   ```

2. **Stain Matrix Application**:
   The optical density values are multiplied by the inverse stain matrix:
   ```
   [H, E, B] = [R, G, B] × INV_STAIN_MATRIX
   ```

3. **Transmittance Conversion**:
   ```
   T = 10^(-concentration)
   ```

### Stain Matrix

The H&E stain matrix uses standard values from Ruifrok & Johnston:

```
Hematoxylin: [0.650, 0.704, 0.286] (Red, Green, Blue)
Eosin:       [0.072, 0.990, 0.105] (Red, Green, Blue)
Background:  [0.000, 0.000, 0.000] (computed as cross product)
```

## Implementation Details

### Class: `HEDeconvolution`

Located in: `scipathj/src/main/java/com/scipath/scipathj/core/analysis/HEDeconvolution.java`

#### Key Features:
- **Ultra-fast matrix operations**: Pre-computed inverse matrix for maximum speed
- **Direct pixel access**: Bypasses ImageJ's pixel access overhead
- **Optimized Gauss-Jordan inversion**: Custom matrix inversion algorithm
- **Automatic cross-product computation**: Background vector computed from H and E vectors

#### Performance Optimizations:
- Pre-computed inverse stain matrix
- Direct byte array manipulation
- Minimal memory allocations
- Fast optical density calculations

### Channel Assignment

The channels are assigned to match Fiji's Color Deconvolution output:
- **Hematoxylin**: Shows nuclei and basophilic structures
- **Eosin**: Shows cytoplasm and eosinophilic structures
- **Background**: Shows the complement (residual)

## Usage

### Direct Usage
```java
// Create deconvolution instance
HEDeconvolution deconvolution = new HEDeconvolution(image);

// Perform deconvolution
deconvolution.performDeconvolution();

// Get individual channels
ImagePlus hematoxylin = deconvolution.getHematoxylinImage();
ImagePlus eosin = deconvolution.getEosinImage();
ImagePlus background = deconvolution.getBackgroundImage();
```

### Test Class
```java
// Run the test
mvn exec:java -Dexec.mainClass=com.scipath.scipathj.core.analysis.HEDeconvolutionTest
```

## Validation

The implementation has been validated against Fiji's Color Deconvolution plugin to ensure identical results. The stain vectors and matrix operations produce the same channel separation as the reference implementation.

### Comparison with Fiji
- Uses identical stain vectors from Ruifrok & Johnston
- Same matrix inversion and optical density calculations
- Channel assignments match Fiji's output order
- Results are visually and numerically equivalent

## Technical Notes

### Matrix Organization
The stain matrix is organized as:
```
[ H_R, H_G, H_B ]
[ E_R, E_G, E_B ]
[ B_R, B_G, B_B ]
```

Where each row represents a stain vector in RGB space.

### Background Vector Computation
The background vector is computed as the cross product of hematoxylin and eosin vectors:
```
B = H × E
```

This ensures the three vectors form an orthonormal basis for color space decomposition.

### Optical Density Conversion
The conversion from RGB to optical density uses:
```
OD = -log10(RGB/255.0)
```

Values less than 1.0/255.0 are clamped to prevent division by zero.

## Dependencies

- ImageJ API (ij.jar)
- SLF4J for logging
- Java 8+ (compiled with Java 23)

## References

1. **Ruifrok, A.C. & Johnston, D.A.** (2001). Quantification of histochemical staining by color deconvolution. *Anal. Quant. Cytol. Histol.*, 23: 291-299.

2. **Landini, G.** (2008). Colour Deconvolution plugin for Fiji/ImageJ. Available at: https://imagej.net/plugins/colour-deconvolution

## Troubleshooting

### Common Issues

1. **Empty channels**: Check if image is RGB color format
2. **Incorrect colors**: Verify stain matrix values match Fiji
3. **Performance issues**: Ensure using latest compiled version

### Debugging
Enable debug logging in `logback.xml` to see detailed deconvolution process:
```xml
<logger name="com.scipath.scipathj.core.analysis.HEDeconvolution" level="DEBUG"/>
```

This will show:
- Matrix computation steps
- Deconvolution timing
- Channel statistics
- Error conditions

## Future Enhancements

Potential improvements:
- Support for custom stain vectors
- Batch processing of multiple images
- GPU acceleration for large images
- Integration with other segmentation algorithms