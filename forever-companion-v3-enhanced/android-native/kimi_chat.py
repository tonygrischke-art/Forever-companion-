from openai import OpenAI

# Initialize the client with Moonshot AI's details
client = OpenAI(
    api_key="YOUR_MOONSHOT_API_KEY", # Replace with your actual key
    base_url="https://api.moonshot.ai/v1",
)

# This list stores the conversation history
history = [
    {"role": "system", "content": "You are Kimi, an AI assistant provided by Moonshot AI."}
]

print("--- Kimi Chat (Type 'exit' to quit) ---")

while True:
    user_input = input("You: ")
    if user_input.lower() in ["exit", "quit"]:
        break

    # Add user message to history
    history.append({"role": "user", "content": user_input})

    # Call the Kimi API
    completion = client.chat.completions.create(
        model="kimi-k2-turbo-preview", # Or "moonshot-v1-8k"
        messages=history,
        temperature=0.3,
    )

    # Get the response and print it
    assistant_msg = completion.choices[0].message.content
    print(f"\nKimi: {assistant_msg}\n")

    # Add assistant response to history to continue the context
    history.append({"role": "assistant", "content": assistant_msg})
