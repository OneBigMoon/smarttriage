3128 platform signing material
==============================

This project needs the vendor platform certificate when building a system-signed APK.

Tracked in this repository:

- `platform.x509.pem`: platform certificate.

Not tracked as a plain file:

- `platform.pk8`: platform private key.

The private key has been uploaded to the GitHub repository secret:

- `PLATFORM_PK8_BASE64`

For local signing, keep the private key outside the repository and run:

```sh
java -jar signapk.jar platform.x509.pem platform.pk8 app.apk app_signed.apk
```

or:

```sh
apksigner sign --key platform.pk8 --cert platform.x509.pem --out app_signed.apk app.apk
```
