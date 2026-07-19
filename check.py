import json
with open('app/src/main/assets/data/evaluation_data.json', 'r', encoding='utf-8') as f:
    data = json.load(f)
for k, v in data.items():
    print(k, len(v.get('pertanyaan_anak', [])), len(v.get('evaluasi', [])))
