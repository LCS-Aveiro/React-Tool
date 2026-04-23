import os

pasta = "shared/src/main/scala/rta"  
ficheiro_saida = "saida.txt"

print(f"Iniciando busca em: {pasta}")

with open(ficheiro_saida, "w", encoding="utf-8") as saida:
    for root, dirs, files in os.walk(pasta):
        for nome in files:
            caminho = os.path.join(root, nome)
            
            if not nome.endswith(".scala"): 
                continue

            print(f"Lendo: {caminho}")
            
            saida.write(f"--- ARQUIVO: {nome} ---\n")
            saida.write(f"Caminho: {caminho}\n")
            saida.write("-" * 30 + "\n")
            
            try:
                with open(caminho, "r", encoding="utf-8") as f:
                    conteudo = f.read()
                saida.write(conteudo + "\n\n")
            except Exception as e:
                print(f"Erro ao ler {nome}: {e}")
                saida.write(f"[Erro ao ler ficheiro: {e}]\n\n")

print(f"✅ Todos os ficheiros (incluindo subpastas) guardados em '{ficheiro_saida}'!")