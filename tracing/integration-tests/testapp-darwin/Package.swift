// swift-tools-version:5.3
import PackageDescription

let package = Package(
   name: "Shared",
   platforms: [
     .macOS(.v15.2),
   ],
   products: [
      .library(name: "Shared", targets: ["Shared"])
   ],
   targets: [
      .binaryTarget(
         name: "Shared",
         url: "<link to the uploaded XCFramework ZIP file>",
         checksum:"<checksum calculated for the ZIP file>")
   ]
)


