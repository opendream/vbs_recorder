#!/bin/bash

# Create directory for binaries if it doesn't exist
mkdir -p target/release/bin

# Build for AMD64 (x86_64)
echo "Building for x86_64-apple-darwin..."
cargo build --release --target x86_64-apple-darwin
cp target/x86_64-apple-darwin/release/qr_generator target/release/bin/qr_generator-x86_64

# Build for ARM64 (aarch64)
echo "Building for aarch64-apple-darwin..."
cargo build --release --target aarch64-apple-darwin
cp target/aarch64-apple-darwin/release/qr_generator target/release/bin/qr_generator-arm64

# Create universal binary (for macOS)
echo "Creating universal binary..."
lipo -create -output target/release/bin/qr_generator \
    target/release/bin/qr_generator-x86_64 \
    target/release/bin/qr_generator-arm64

echo "Build complete! Binaries are in target/release/bin/"
