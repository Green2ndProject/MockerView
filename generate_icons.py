from PIL import Image, ImageDraw, ImageFont
import os

os.makedirs('src/main/resources/static/images', exist_ok=True)

sizes = [72, 96, 128, 144, 152, 192, 384, 512]

for size in sizes:
    img = Image.new('RGB', (size, size), color='#667eea')
    draw = ImageDraw.Draw(img)
    
    try:
        font_size = int(size * 0.4)
        font = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial Bold.ttf", font_size)
    except:
        font = ImageFont.load_default()
    
    text = "M"
    bbox = draw.textbbox((0, 0), text, font=font)
    text_width = bbox[2] - bbox[0]
    text_height = bbox[3] - bbox[1]
    
    x = (size - text_width) // 2
    y = (size - text_height) // 2 - int(size * 0.05)
    
    draw.text((x, y), text, fill='white', font=font)
    
    img.save(f'src/main/resources/static/images/icon-{size}x{size}.png')
    print(f'Generated icon-{size}x{size}.png')

print('All icons generated successfully!')
