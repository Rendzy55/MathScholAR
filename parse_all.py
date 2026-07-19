
import json
import re

def parse_file(filename):
    with open(filename, 'r', encoding='utf-8') as f:
        content = f.read()

    # Split into Soal Evaluasi and Soal PBL
    # Look for 'Tahap 1 - Memahami Masalah'
    split_index = content.find('Tahap 1 - Memahami Masalah')
    if split_index != -1:
        # Also include the preceding text 'Pertanyaan X:' if it's there
        # Usually 'Script Chatbot AI - Latihan Soal PBL' is right before it
        evaluasi_text = content[:split_index]
        pbl_text = content[split_index:]
    else:
        evaluasi_text = content
        pbl_text = ''

    def extract_questions(text):
        questions = []
        blocks = re.split(r'Pertanyaan\s*(?:\d+)?\s*:', text)[1:]
        
        for i, block in enumerate(blocks):
            q_dict = {}
            q_dict['id'] = i + 1
            
            m = re.search(r'^(.*?)(?:Kata Kunci:)', block, re.DOTALL | re.IGNORECASE)
            if m:
                q_dict['pertanyaan'] = m.group(1).strip()
            
            m = re.search(r'Kata Kunci:\s*(.*?)(?:\nJawaban Benar:)', block, re.DOTALL | re.IGNORECASE)
            if m:
                keys = m.group(1).strip()
                keys = re.split(r'[,;]', keys)
                q_dict['kata_kunci'] = [k.strip() for k in keys if k.strip()]
            
            m = re.search(r'Feedback (?:Jika )?Benar:\s*(.*?)(?:\nJawaban Sebagian Benar:)', block, re.DOTALL | re.IGNORECASE)
            feedback_benar = m.group(1).strip() if m else ''
            
            m = re.search(r'Feedback (?:Jika )?Sebagian(?: Benar)?:\s*(.*?)(?:\nJawaban Kurang Tepat:)', block, re.DOTALL | re.IGNORECASE)
            feedback_sebagian = m.group(1).strip() if m else ''
            
            m = re.search(r'Feedback (?:Jika )?(?:Kurang Tepat|Salah):\s*(.*?)(?:\nJika (?:p|P)ercobaan (?:k|K)edua)', block, re.DOTALL | re.IGNORECASE)
            feedback_salah = m.group(1).strip() if m else ''
            
            q_dict['feedback'] = {
                'benar': feedback_benar,
                'sebagian': feedback_sebagian,
                'salah': feedback_salah
            }
            
            m = re.search(r'Jika (?:p|P)ercobaan (?:k|K)edua.*?:\s*(.*)', block, re.DOTALL | re.IGNORECASE)
            if m:
                jb = m.group(1).strip()
                q_dict['jawaban_benar'] = jb
            else:
                q_dict['jawaban_benar'] = ''
            
            questions.append(q_dict)
        return questions

    pertanyaan_anak = extract_questions(evaluasi_text)
    evaluasi = extract_questions(pbl_text)
    
    return {
        'pertanyaan_anak': pertanyaan_anak,
        'evaluasi': evaluasi
    }

data = {
    'kubus': parse_file('Soal dan Evaluasi Kubus.txt'),
    'balok': parse_file('Soal dan Evaluasi Balok.txt'),
    'prisma': parse_file('Soal dan Evaluasi Prisma.txt')
}

with open('app/src/main/assets/data/evaluation_data.json', 'w', encoding='utf-8') as f:
    json.dump(data, f, indent=2, ensure_ascii=False)

