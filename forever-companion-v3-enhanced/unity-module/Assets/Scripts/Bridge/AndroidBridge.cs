using UnityEngine;
using System;
using Newtonsoft.Json.Linq;

namespace ForeverCompanion.Bridge
{
    public class AndroidBridge : MonoBehaviour
    {
        public static AndroidBridge Instance { get; private set; }

        public event Action<string, float> OnMoodDelta;
        public event Action<string, int> OnAnimationTrigger;
        public event Action<string, string> OnAvatarUpdate;
        public event Action<Vector3> OnLookTarget;

        private void Awake()
        {
            if (Instance != null) { Destroy(gameObject); return; }
            Instance = this;
            DontDestroyOnLoad(gameObject);
            NotifyAndroidReady();
        }

        public void ReceiveDelta(string json)
        {
            try
            {
                var msg = JObject.Parse(json);
                var type = msg["type"].Value<string>();
                var payload = msg["payload"];

                switch (type)
                {
                    case "MOOD_DELTA":
                        OnMoodDelta?.Invoke(payload["mood"].Value<string>(), 
                            payload["intensity"].Value<float>());
                        break;
                    case "ANIM_TRIGGER":
                        OnAnimationTrigger?.Invoke(payload["animation"].Value<string>(),
                            payload["layer"].Value<int>());
                        break;
                    case "AVATAR_UPDATE":
                        OnAvatarUpdate?.Invoke(payload["glbUrl"].Value<string>(),
                            payload["outfitId"]?.Value<string>());
                        break;
                    case "LOOK_AT":
                        OnLookTarget?.Invoke(new Vector3(
                            payload["x"].Value<float>(),
                            payload["y"].Value<float>(),
                            payload["z"].Value<float>()));
                        break;
                }
            }
            catch (Exception e)
            {
                Debug.LogError($"Delta error: {e.Message}");
            }
        }

        public void RequestPurchase(string productId)
        {
            var msg = new JObject { ["action"] = "REQUEST_PURCHASE", ["productId"] = productId };
            SendToAndroid(msg.ToString());
        }

        private void NotifyAndroidReady()
        {
            SendToAndroid(new JObject { ["action"] = "UNITY_READY" }.ToString());
        }

        private void SendToAndroid(string json)
        {
            #if UNITY_ANDROID && !UNITY_EDITOR
            using (var bridge = new AndroidJavaClass("com.forevercompanion.bridge.UnityBridge"))
            {
                bridge.CallStatic("receiveFromUnity", json);
            }
            #else
            Debug.Log($"[ToAndroid] {json}");
            #endif
        }
    }
}
