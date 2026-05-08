@echo off
echo Starting AI Service...
cd ai_service
pip install -r requirements.txt
python main.py
pause
