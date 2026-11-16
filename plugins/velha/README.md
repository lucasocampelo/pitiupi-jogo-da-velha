# Plugin Jogo da Velha (pitiupi)

Plugin do pitiupi para dois peers jogarem Jogo da Velha por **comunicação individual (unicast)**.

## Protocolo

### Transporte

- **Unicast TCP**, com `ConnectionManager.sendTcp(mensagem, ipDoAdversario)`.
- TCP foi escolhido porque garante entrega e ordem. Se uma jogada se perdesse, os tabuleiros ficariam diferentes.
- A serialização e os sockets são do núcleo do pitiupi; o plugin não abre sockets.

### Mensagem

Uma classe, `VelhaMessage` (estende `pitiupi.models.Messages.Message`):

| Campo | Tipo | Descrição |
|---|---|---|
| `tipo` | enum | `CONVITE`, `ACEITE`, `RECUSA` ou `JOGADA` |
| `nome` | String | Nome de quem enviou |
| `instancia` | String | Id da instância do pitiupi de quem enviou |
| `ip` | String | IPv4 de quem enviou |
| `posicao` | int | Só em `JOGADA`: casa de 0 a 8 |

Casas do tabuleiro:

```
 0 | 1 | 2
 3 | 4 | 5
 6 | 7 | 8
```

### Tipos

| Tipo | Enviado por | Significado |
|---|---|---|
| `CONVITE` | Quem quer jogar | Pede uma partida. Quem convida é **X** e começa |
| `ACEITE` | Convidado | Aceita; a partida começa. O convidado é **O** |
| `RECUSA` | Convidado | Recusa o convite |
| `JOGADA` | Jogador da vez | Marca a casa `posicao` |

### Regras

- O receptor de um `CONVITE` descobre o IP do remetente procurando `instancia` na lista de presença do núcleo; se não achar, usa o campo `ip`.
- Um `CONVITE` só é aceito se o peer não estiver em outra partida.
- Uma `JOGADA` recebida só é aplicada se for a vez do adversário e a casa estiver vazia.
- X joga nas jogadas pares e O nas ímpares.
- Os dois lados aplicam as mesmas jogadas, então cada um detecta sozinho vitória (3 em linha, coluna ou diagonal) ou empate (9 jogadas). Não existe mensagem de fim de jogo.

### Estados

```
LIVRE ──envia CONVITE──▶ AGUARDANDO ──recebe ACEITE──▶ JOGANDO ──▶ FIM
  │                          └──recebe RECUSA──▶ LIVRE
  └──recebe CONVITE──▶ CONVITE_RECEBIDO ──envia ACEITE──▶ JOGANDO
                                └──envia RECUSA──▶ LIVRE
```

De `FIM` é possível convidar ou ser convidado de novo.

### Exemplo

```
Ana                               Bia
 │── CONVITE ───────────────────▶│
 │◀────────────────────── ACEITE ─│
 │── JOGADA pos=0 (X) ──────────▶│
 │◀───────────── JOGADA pos=3 (O)─│
 │── JOGADA pos=1 (X) ──────────▶│
 │◀───────────── JOGADA pos=4 (O)─│
 │── JOGADA pos=2 (X) ──────────▶│   Ana venceu (linha 0-1-2)
```

## Compilar e rodar

```bash
cd pitiupi && ant jar && cd ..          # núcleo
cd plugins/velha && ant jar && cd ../.. # plugin -> dist/pitiupi.velha.jar
./scripts/pitiupi.sh link --all
./scripts/pitiupi.sh run
```

Entrada em `plugins.registry.json`:

```json
{ "id": "velha", "dir": "plugins/velha", "build": "ant", "jar": "dist/pitiupi.velha.jar", "menuCategory": "Jogos" }
```

## Uso

Abra o pitiupi em dois computadores da mesma rede, na mesma porta. Na aba **Jogo da Velha**, escolha o adversário e clique em **Convidar**. O outro lado clica em **Aceitar**, e depois cada um joga na sua vez.
