
def normalizeText(text):
    text = text.lower()
    text = text.replace('×', 'x').replace('?', 'x').replace('*', 'x').replace('kali', 'x')
    import re
    text = re.sub(r'\s+', ' ', text)
    return text.strip()

def getSynonyms(keyword):
    synonymMap = {
        'bangun ruang': ['bangun 3 dimensi', 'bangun tiga dimensi', 'bentuk 3d', 'bentuk tiga dimensi'],
        'persegi': ['kotak', 'segi empat sama sisi'],
        'sama besar': ['sama ukuran', 'ukuran sama', 'sama panjang', 'kongruen', 'sama luas'],
        '6 sisi': ['enam sisi', '6 buah sisi', 'enam buah sisi'],
        '12 rusuk': ['dua belas rusuk', '12 buah rusuk'],
        '8 titik sudut': ['delapan titik sudut', '8 buah titik sudut'],
        's x s': ['sisi x sisi', 's pangkat 2', 's kuadrat', 's²'],
        's x s x s': ['sisi x sisi x sisi', 's pangkat 3', 's kubik', 's³'],
        '6 x s x s': ['6s²', '6 s²', 'enam x s x s', '6 x sisi x sisi'],
        'panjang x lebar x tinggi': ['p x l x t', 'panjang kali lebar kali tinggi'],
        'sisi sama panjang': ['semua sisi sama', 'ukuran sisi sama', 'rusuk sama panjang'],
        'semua sisi kubus terlihat': ['sisi terlihat semua', 'seluruh sisi terlihat', 'semua sisi terlihat', 'semua sisi terbuka'],
        'luas seluruh permukaan dapat dihitung': ['bisa dihitung luas', 'menghitung luas semua sisi', 'luas semua sisi dihitung', 'menghitung seluruh luas permukaan'],
        'persegi panjang': ['kotak panjang', 'segi empat'],
        'pasangan berhadapan sama besar': ['sisi yang berhadapan sama', 'berhadapan ukurannya sama', 'pasangan sisi sama besar', 'ukuran berhadapannya sama'],
        'semua sisi balok terlihat': ['semua sisi terlihat', 'sisi terlihat semua', 'seluruh sisi terbuka', 'dibuka semua sisinya'],
        'p, l, t': ['panjang, lebar, dan tinggi', 'panjang, lebar, tinggi', 'p, l, dan t'],
        '3 pasang sisi': ['tiga pasang sisi', 'tiga sisi berpasangan'],
        '2 x ((p x l) + (l x t) + (p x t))': ['2 x (pl + lt + pt)', '2x(p x l + l x t + p x t)', '2*(p*l + l*t + p*t)', '2 x (p x l + l x t + p x t)'],
        'berjejer ke samping, belakang, dan atas': ['berjejer ke samping, ke belakang, dan ke atas', 'tumpukan ke samping, ke belakang, dan ke atas', 'menyamping, ke belakang, dan ke atas'],
        'kapasitas muatan': ['isi kotak', 'isi ruang', 'muatan di dalam kotak', 'isi dalamnya']
    }
    return synonymMap.get(keyword, [])

def matchKeyword(normalizedAnswer, keyword):
    normalizedKeyword = normalizeText(keyword)
    if normalizedKeyword in normalizedAnswer:
        return True
    synonyms = getSynonyms(normalizedKeyword)
    for synonym in synonyms:
        if synonym in normalizedAnswer:
            return True
    return False

def evaluate(answer, keywords):
    normalizedAnswer = normalizeText(answer)
    matchCount = 0
    for keyword in keywords:
        if matchKeyword(normalizedAnswer, keyword):
            matchCount += 1
    percentage = (matchCount / len(keywords)) * 100.0 if keywords else 100.0
    return matchCount, percentage

keywords = ['6 sisi', '12 rusuk', '8 titik sudut']
answer = '6 sisi, 12 rusuk dan 8 sudut'
print(evaluate(answer, keywords))

