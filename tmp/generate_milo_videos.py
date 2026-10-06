import os
import math
import subprocess
import sys

def create_mp4_video(output_path, state_name, title, emoji, color_rgb):
    width, height = 360, 360
    fps = 30
    duration_sec = 2 # 60 frames loop
    total_frames = fps * duration_sec

    r_c, g_c, b_c = color_rgb

    # Start ffmpeg process to encode raw RGB24 stream into H.264 MP4
    cmd = [
        'ffmpeg',
        '-y',
        '-f', 'rawvideo',
        '-vcodec', 'rawvideo',
        '-s', f'{width}x{height}',
        '-pix_fmt', 'rgb24',
        '-r', str(fps),
        '-i', '-',
        '-c:v', 'libx264',
        '-pix_fmt', 'yuv420p',
        '-profile:v', 'baseline',
        '-level', '3.0',
        '-tune', 'stillimage',
        '-movflags', '+faststart',
        output_path
    ]

    proc = subprocess.Popen(cmd, stdin=subprocess.PIPE, stderr=subprocess.DEVNULL)

    cx, cy = width / 2.0, height / 2.0
    outer_radius = 160.0

    for frame in range(total_frames):
        t = frame / float(total_frames)
        angle_t = t * 2.0 * math.pi
        pulse = math.sin(angle_t)

        frame_bytes = bytearray(width * height * 3)
        idx = 0

        # Dynamic animations based on state
        orbit_angle = angle_t
        orbit_r = 110.0 + 10.0 * math.sin(angle_t * 2)

        p1_x = cx + orbit_r * math.cos(orbit_angle)
        p1_y = cy + orbit_r * math.sin(orbit_angle)

        p2_x = cx + orbit_r * math.cos(orbit_angle + math.pi)
        p2_y = cy + orbit_r * math.sin(orbit_angle + math.pi)

        milo_pulse_r = 85.0 + 6.0 * pulse

        for y in range(height):
            dy = y - cy
            for x in range(width):
                dx = x - cx
                dist = math.sqrt(dx * dx + dy * dy)

                if dist > outer_radius:
                    # Pure crisp dark background (no green screen!)
                    r, g, b = 15, 23, 42
                else:
                    # Inside circle: Smooth gradient backdrop
                    bg_factor = (dist / outer_radius)
                    r = int(255 * (1 - bg_factor) + 20 * bg_factor)
                    g = int(255 * (1 - bg_factor) + 30 * bg_factor)
                    b = int(255 * (1 - bg_factor) + 50 * bg_factor)

                    # Pulse energy ring
                    ring_dist = abs(dist - (120.0 + 12.0 * pulse))
                    if ring_dist < 6.0:
                        rf = (1.0 - ring_dist / 6.0)
                        r = int(r * (1 - rf) + r_c * rf)
                        g = int(g * (1 - rf) + g_c * rf)
                        b = int(b * (1 - rf) + b_c * rf)

                    # Milo Mascot Central Golden Lion Body
                    if dist <= milo_pulse_r:
                        m_dist_norm = dist / milo_pulse_r
                        # Golden fur gradient
                        m_r = int(251 * (1 - m_dist_norm * 0.3))
                        m_g = int(191 * (1 - m_dist_norm * 0.4))
                        m_b = int(36 * (1 - m_dist_norm * 0.5))

                        # Lion face details: Eyes & Nose
                        eye1_d = math.sqrt((dx + 22)**2 + (dy + 15)**2)
                        eye2_d = math.sqrt((dx - 22)**2 + (dy + 15)**2)
                        nose_d = math.sqrt((dx)**2 + (dy - 10)**2)

                        if eye1_d < 7.0 or eye2_d < 7.0:
                            m_r, m_g, m_b = 30, 41, 59 # Dark eyes
                            # Eye shine
                            if math.sqrt((dx + 20)**2 + (dy + 17)**2) < 2.5 or math.sqrt((dx - 24)**2 + (dy + 17)**2) < 2.5:
                                m_r, m_g, m_b = 255, 255, 255
                        elif nose_d < 10.0:
                            m_r, m_g, m_b = 225, 29, 72 # Cute pink nose

                        r, g, b = m_r, m_g, m_b

                    # Lion ears
                    ear1_d = math.sqrt((dx + 65)**2 + (dy + 65)**2)
                    ear2_d = math.sqrt((dx - 65)**2 + (dy + 65)**2)
                    if ear1_d < 28.0 or ear2_d < 28.0:
                        r, g, b = 245, 158, 11 # Golden ear fur

                    # Orbiting sparkle particles
                    d_p1 = math.sqrt((x - p1_x)**2 + (y - p1_y)**2)
                    d_p2 = math.sqrt((x - p2_x)**2 + (y - p2_y)**2)
                    if d_p1 < 8.0:
                        rf = (1.0 - d_p1 / 8.0)
                        r = int(r * (1 - rf) + 255 * rf)
                        g = int(g * (1 - rf) + 220 * rf)
                        b = int(b * (1 - rf) + 100 * rf)
                    elif d_p2 < 8.0:
                        rf = (1.0 - d_p2 / 8.0)
                        r = int(r * (1 - rf) + r_c * rf)
                        g = int(g * (1 - rf) + g_c * rf)
                        b = int(b * (1 - rf) + b_c * rf)

                frame_bytes[idx] = min(255, max(0, r))
                frame_bytes[idx+1] = min(255, max(0, g))
                frame_bytes[idx+2] = min(255, max(0, b))
                idx += 3

        proc.stdin.write(frame_bytes)

    proc.stdin.close()
    proc.wait()
    print(f"Generated MP4: {output_path}")

raw_dir = "/app/src/main/res/raw"
os.makedirs(raw_dir, exist_ok=True)

states = [
    ("milo_idle.mp4", "Idle", "🦁", (25, 118, 210)),
    ("milo_welcome.mp4", "Welcome", "👋", (37, 99, 235)),
    ("milo_working.mp4", "Working", "💻", (2, 132, 199)),
    ("milo_thinking.mp4", "Thinking", "💡", (217, 119, 6)),
    ("milo_lead_imported.mp4", "Lead Imported", "📥", (37, 99, 235)),
    ("milo_new_lead.mp4", "New Lead", "✨", (124, 58, 237)),
    ("milo_follow_up.mp4", "Follow Up", "📞", (5, 150, 105)),
    ("milo_success.mp4", "Success", "👍", (16, 185, 129)),
    ("milo_converted.mp4", "Converted", "🏆", (234, 179, 8)),
    ("milo_warning.mp4", "Warning", "⚠️", (234, 88, 12)),
    ("milo_error.mp4", "Error", "❌", (220, 38, 38)),
    ("milo_goodbye.mp4", "Goodbye", "🙋", (71, 85, 105)),
    ("milo_celebration.mp4", "Celebration", "🎉", (139, 92, 246)),
    ("milo_splash.mp4", "Splash", "🚀", (37, 99, 235)),
    ("milo_status.mp4", "Status", "🦁", (25, 118, 210)),
    ("milo_video.mp4", "Video", "🦁", (25, 118, 210))
]

for filename, title, emoji, color in states:
    path = os.path.join(raw_dir, filename)
    create_mp4_video(path, filename, title, emoji, color)

print("All Milo MP4 assets successfully generated in res/raw!")
