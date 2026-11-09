# framed

native android app that puts photos into frames and prints their exif data on the card: camera, lens, exposure, date, and the brand logo. kotlin and jetpack compose. the app has no internet permission, so photos stay on the device.

## features

- six frame styles: cinematic blur, studio passe-partout, left sidebar, gallery minimal, vintage polaroid, editorial split.
- exif parsing: make, model, lens, focal length, aperture, shutter speed, iso, capture date. sony model codes turn into readable names (`ILCE-7M4` becomes `α7 IV`).
- brand logos: sony, sony alpha, nikon, canon, fujifilm, leica, hasselblad, lumix, apple. the logo can be white, black, brand colored, or match the text.
- canvas ratios: 4:5, 9:16, 1:1, 3:4, 16:9, or the original one.
- background: blurred photo or a solid color palette, with corner radius, shadow, and border controls.
- typography: inter, space grotesk, playfair, jetbrains mono, with separate sizes, spacing, offsets, and alignment.
- batch mode: pick several shots and one style applies to all of them. the app also accepts images from the android share menu.
- export: full resolution jpeg at quality 100, or a social media version with the long side capped at 2560 px.
- gallery of saved works.

## requirements

android 8.0 or newer.

## building

needs jdk 21 and android sdk 36.

```sh
git clone https://github.com/waydef/framed.git
cd framed
./gradlew assembleDebug
```

## кратко на русском

нативное android-приложение на kotlin и jetpack compose: вставляет фото в рамки и печатает на карточке данные exif (камера, объектив, экспозиция, дата) с логотипом бренда. шесть стилей рамок, пакетная обработка, экспорт в полном разрешении или версией для соцсетей. разрешения на интернет у приложения нет, фото остаются на устройстве.
