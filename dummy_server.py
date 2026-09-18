import asyncio
import websockets

async def handler(websocket):
    port = websocket.local_address[1]
    print(f"[Porta {port}] Un agente si è connesso con successo.")
    try:
        async for message in websocket:
            pass 
    except websockets.exceptions.ConnectionClosed:
        print(f"[Porta {port}] Un agente si è disconnesso.")

async def main():
    print("Avvio dei server")
    print("In ascolto sulle porte: 9080, 9081, 9082, 9083")
    
    # Apriamo 4 server in parallelo, uno per ogni agente
    async with websockets.serve(handler, "localhost", 9080), \
               websockets.serve(handler, "localhost", 9081), \
               websockets.serve(handler, "localhost", 9082), \
               websockets.serve(handler, "localhost", 9083):
        await asyncio.Future() 

if __name__ == "__main__":
    asyncio.run(main())