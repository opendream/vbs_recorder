use qrcode::QrCode;
use qrcode::render::unicode;
use serde_json::Value;
use std::io::{self, Read};
use clap::Parser;
use image::{ImageBuffer, Luma};

#[derive(Parser, Debug)]
#[command(author, version, about, long_about = None)]
struct Args {
    /// Output PNG file path (optional)
    #[arg(short, long)]
    output: Option<String>,

    /// QR code size in pixels (for PNG output)
    #[arg(short, long, default_value_t = 200)]
    size: u32,
}

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let args = Args::parse();

    // Read input from stdin
    let mut input = String::new();
    io::stdin().read_to_string(&mut input)?;
    
    // Trim whitespace
    let input = input.trim();
    
    // Try to parse as JSON first
    let text = match serde_json::from_str::<Value>(input) {
        Ok(json) => json.to_string(),
        Err(_) => input.to_string(), // If not valid JSON, use raw input
    };
    
    // Create QR code
    let code = QrCode::new(text.as_bytes())?;
    
    // Handle PNG output if requested
    if let Some(output_path) = args.output {
        generate_png(&code, &output_path, args.size)?;
        println!("QR code saved to: {}", output_path);
    } else {
        // Print QR code to terminal
        let qr_string = code.render::<unicode::Dense1x2>()
            .dark_color(unicode::Dense1x2::Light)
            .light_color(unicode::Dense1x2::Dark)
            .build();
        
        println!("\nGenerated QR Code:\n");
        println!("{}", qr_string);
    }
    
    Ok(())
}

fn generate_png(code: &QrCode, path: &str, size: u32) -> Result<(), Box<dyn std::error::Error>> {
    let code_pixels = code.to_vec();
    let width = (code_pixels.len() as f32).sqrt() as u32;
    let scale = size / width;
    let size = width * scale; // Ensure size is a multiple of QR code size
    
    // Create a new image buffer
    let mut image = ImageBuffer::new(size, size);
    
    // Generate the QR code pixels
    for (i, &module) in code_pixels.iter().enumerate() {
        let x = (i as u32 % width) * scale;
        let y = (i as u32 / width) * scale;
        
        let color = if module {
            Luma([0u8]) // Black
        } else {
            Luma([255u8]) // White
        };
        
        // Scale up each QR code module
        for sy in 0..scale {
            for sx in 0..scale {
                image.put_pixel(
                    x + sx,
                    y + sy,
                    color
                );
            }
        }
    }
    
    // Save the image
    image.save(path)?;
    Ok(())
}
