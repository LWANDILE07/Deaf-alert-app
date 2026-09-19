# Acoustic Guardian

An accessibility app that listens for emergency sounds — smoke alarms, breaking glass — and alerts Deaf and hard-of-hearing users through flashlight, vibration, and automatic SMS to emergency contacts.

## The Problem
Standard smoke alarms and emergency sounds are auditory-only, leaving Deaf and hard-of-hearing individuals without warning during a fire, break-in, or other emergency — especially while asleep or without hearing aids. Existing assistive devices (bed-shakers, dedicated strobe alarms) require buying and installing extra hardware.

## Our Solution
Acoustic Guardian turns a smartphone people already own into a listening safety hub — no extra hardware required.

**How it works:**
1. Listens continuously for emergency sounds using Google's pretrained YAMNet model
2. Requires multiple consecutive detections before triggering, to reduce false positives
3. On detection: vibrates, flashes the camera light, and sends an SMS with live location to saved emergency contacts
4. User can press "I'm safe" to cancel and notify contacts the alert was a false alarm

## Features
- Real-time background sound detection (works while the phone is locked)
- Multi-sensory alerts: vibration, flashlight, SMS
- Emergency contacts management (stored locally on-device)
- Live location sharing via Google Maps link
- False-positive reduction via consecutive-detection logic

## Tech Stack
- Android (Java)
- TensorFlow Lite Task Library + Google's pretrained YAMNet model for sound classification
- Android foreground services for background microphone access
- SharedPreferences (local JSON storage) for contacts
- Google Play Services (FusedLocationProviderClient) for location

## Attribution
Sound classification uses Google's open-source [YAMNet](https://tfhub.dev/google/yamnet) model via the TensorFlow Lite Task Library. All application logic, UI, and the detection/alert pipeline were built by our team.

## Limitations
- Requires the phone to be nearby, charged, and running in the background
- Detection accuracy depends on background noise and alarm type
- SMS to emergency contacts requires the user to have airtime/SMS credit — the user themself is still alerted via vibration and flashlight regardless

## Roadmap
- WhatsApp automation
- Broader alarm-sound training data
- iOS support

## Team
- **Rethabile** — UI/UX Design
- **Lwandile** — Audio Detection
- **Nobuhle** — Integration
