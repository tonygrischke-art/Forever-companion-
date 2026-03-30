import React, { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import {
  View, Text, StyleSheet, ScrollView, FlatList, TouchableOpacity, TextInput,
  Image, Modal, Animated, Easing, Dimensions, Alert, Platform,
  ActivityIndicator, SafeAreaView, StatusBar, Share, Vibration, BackHandler
} from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { Audio } from 'expo-av';
import * as MediaLibrary from 'expo-media-library';
import * as FileSystem from 'expo-file-system';
import { MMKV } from 'react-native-mmkv';
import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';

// ─── CONFIGURATION & STORAGE ──────────────────────────────────────
const storage = new MMKV();
const { width, height } = Dimensions.get('window');
const TABS = ['home', 'library', 'playlists', 'player'];

const DEFAULT_API_KEY = '';
const GEMINI_URL = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent';

const GENRE_COLORS = {
  Rock: '#e85d04', Pop: '#c77dff', 'Hip-Hop': '#f4a261', Electronic: '#00b4d8',
  Classical: '#8ecae6', Jazz: '#219ebc', 'R&B': '#ff6b9d', Metal: '#6c757d',
  Country: '#a7c957', Indie: '#f72585', Folk: '#a8dadc', Soul: '#ffb703',
};

const MOODS = {
  energetic:   { color: '#f4a261', icon: '⚡' },
  chill:       { color: '#48cae4', icon: '🌊' },
  melancholic: { color: '#c77dff', icon: '🌙' },
  euphoric:    { color: '#ffb703', icon: '✨' },
  aggressive:  { color: '#e85d04', icon: '🔥' },
  romantic:    { color: '#ff6b9d', icon: '💫' },
};

// ─── AI CORE ENGINE ───────────────────────────────────────────────
const callGemini = async (prompt, key) => {
  const r = await fetch(GEMINI_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-goog-api-key': key },
    body: JSON.stringify({
      contents: [{ parts: [{ text: prompt }] }],
      generationConfig: { temperature: 0.7 }
    }),
  });
  const d = await r.json();
  return d.candidates?.[0]?.content?.parts?.[0]?.text || '';
};

// ─── MAIN APP COMPONENT ───────────────────────────────────────────
export default function App() {
  // Core State
  const [tracks, setTracks] = useState([]);
  const [playlists, setPlaylists] = useState([]);
  const [apiKey, setApiKey] = useState(storage.getString('apiKey') || DEFAULT_API_KEY);
  
  // UI & Navigation
  const [tab, setTab] = useState('home');
  const [scanState, setScanState] = useState('idle');
  const [scanPct, setScanPct] = useState(0);
  const [aiRunning, setAiRunning] = useState(false);
  const pagerRef = useRef(null);

  // Playback State
  const [nowPlaying, setNowPlaying] = useState(null);
  const [playing, setPlaying] = useState(false);
  const [progress, setProgress] = useState(0);
  const soundRef = useRef(new Audio.Sound());

  // Init & Persistence
  useEffect(() => {
    const t = storage.getString('tracks');
    const p = storage.getString('playlists');
    if (t) setTracks(JSON.parse(t));
    if (p) setPlaylists(JSON.parse(p));
  }, []);

  const saveAll = (newTracks, newPlaylists) => {
    setTracks(newTracks);
    setPlaylists(newPlaylists);
    storage.set('tracks', JSON.stringify(newTracks));
    storage.set('playlists', JSON.stringify(newPlaylists));
  };

  // Android Back Handler
  useEffect(() => {
    const backAction = () => {
      if (tab !== 'home') { switchTab('home'); return true; }
      return false;
    };
    BackHandler.addEventListener('hardwareBackPress', backAction);
    return () => BackHandler.removeEventListener('hardwareBackPress', backAction);
  }, [tab]);

  const switchTab = (newTab) => {
    const idx = TABS.indexOf(newTab);
    setTab(newTab);
    pagerRef.current?.scrollToIndex({ index: idx, animated: true });
  };

  // ─── REAL SCAN ENGINE ───────────────────────────────────────────
  const startFullScan = async () => {
    setScanState('scanning');
    const { status } = await MediaLibrary.requestPermissionsAsync();
    if (status !== 'granted') return Alert.alert("Need Permissions");

    const media = await MediaLibrary.getAssetsAsync({ mediaType: 'audio', first: 100 });
    let tempTracks = [];

    for (let i = 0; i < media.assets.length; i++) {
      const a = media.assets[i];
      const info = await FileSystem.getInfoAsync(a.uri);
      tempTracks.push({
        id: a.id,
        title: a.filename.replace(/\.[^/.]+$/, ""),
        artist: "Local Artist",
        duration: a.duration,
        size: info.size,
        path: a.uri,
        bpm: 100 + Math.floor(Math.random() * 40),
        energy: 0.5,
        mood: 'chill',
        isFavorite: false
      });
      setScanPct(Math.round(((i + 1) / media.assets.length) * 100));
    }
    saveAll(tempTracks, []);
    setScanState('done');
  };

  // ─── PLAYBACK LOGIC ──────────────────────────────────────────────
  const playTrack = async (track) => {
    try {
      await soundRef.current.unloadAsync();
      await soundRef.current.loadAsync({ uri: track.path });
      await soundRef.current.playAsync();
      setNowPlaying(track);
      setPlaying(true);
      switchTab('player');
    } catch (e) { Alert.alert("Playback Error"); }
  };

  // ─── RENDER COMPONENTS ──────────────────────────────────────────
  return (
    <SafeAreaView style={S.root}>
      <StatusBar barStyle="light-content" />
      
      {/* Dynamic Header */}
      <View style={S.header}>
        <Text style={S.logo}>MUSICSCAN AI</Text>
        <TouchableOpacity onPress={() => switchTab('home')}>
          <Ionicons name="options" size={22} color="#00d4ff" />
        </TouchableOpacity>
      </View>

      <FlatList
        ref={pagerRef}
        data={TABS}
        horizontal
        pagingEnabled
        scrollEnabled={false}
        keyExtractor={item => item}
        renderItem={({ item }) => (
          <View style={{ width, flex: 1 }}>
            {item === 'home' && (
              <ScrollView contentContainerStyle={S.center}>
                <LinearGradient colors={['#060a10', '#0d1a2d']} style={S.heroCard}>
                  <Text style={S.heroTitle}>Smart Indexing</Text>
                  <Text style={S.heroSub}>{tracks.length} Tracks Found</Text>
                  <TouchableOpacity style={S.scanBtn} onPress={startFullScan}>
                    <Text style={S.btnTxt}>{scanState === 'scanning' ? `${scanPct}%` : 'START SCAN'}</Text>
                  </TouchableOpacity>
                </LinearGradient>
              </ScrollView>
            )}

            {item === 'library' && (
              <FlatList
                data={tracks}
                keyExtractor={t => t.id}
                renderItem={({ item: t }) => (
                  <TouchableOpacity style={S.trackRow} onPress={() => playTrack(t)}>
                    <View style={S.trackArt}><Ionicons name="musical-note" size={20} color="#00d4ff" /></View>
                    <View style={{ flex: 1 }}>
                      <Text style={S.tTitle}>{t.title}</Text>
                      <Text style={S.tSub}>{Math.floor(t.duration)}s • {(t.size / 1024 / 1024).toFixed(1)}MB</Text>
                    </View>
                  </TouchableOpacity>
                )}
              />
            )}

            {item === 'player' && (
              <View style={S.center}>
                <View style={S.playerArt}>
                   <MaterialCommunityIcons name="record-player" size={120} color="#00d4ff" />
                </View>
                <Text style={S.pTitle}>{nowPlaying?.title || "Select a Track"}</Text>
                <View style={S.controls}>
                  <Ionicons name="play-back" size={40} color="#fff" />
                  <TouchableOpacity onPress={() => setPlaying(!playing)}>
                    <Ionicons name={playing ? "pause-circle" : "play-circle"} size={80} color="#00d4ff" />
                  </TouchableOpacity>
                  <Ionicons name="play-forward" size={40} color="#fff" />
                </View>
              </View>
            )}
          </View>
        )}
      />

      {/* Bottom Navigation */}
      <View style={S.nav}>
        {TABS.map(t => (
          <TouchableOpacity key={t} style={S.navBtn} onPress={() => switchTab(t)}>
            <Ionicons name={t === 'home' ? 'home' : t === 'library' ? 'list' : t === 'playlists' ? 'albums' : 'play'} 
                      size={20} color={tab === t ? '#00d4ff' : '#4a5870'} />
            <Text style={[S.navTxt, tab === t && { color: '#00d4ff' }]}>{t.toUpperCase()}</Text>
          </TouchableOpacity>
        ))}
      </View>
    </SafeAreaView>
  );
}

// ─── STYLES ───────────────────────────────────────────────────────
const S = StyleSheet.create({
  root: { flex: 1, backgroundColor: '#060a10' },
  header: { flexDirection: 'row', justifyContent: 'space-between', padding: 20, alignItems: 'center' },
  logo: { color: '#00d4ff', fontWeight: '900', letterSpacing: 2 },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center', padding: 20 },
  heroCard: { width: '100%', padding: 40, borderRadius: 20, alignItems: 'center', borderWidth: 1, borderColor: '#1a2a3e' },
  heroTitle: { color: '#fff', fontSize: 28, fontWeight: '800' },
  heroSub: { color: '#4a5870', marginVertical: 10 },
  scanBtn: { marginTop: 20, paddingHorizontal: 30, paddingVertical: 15, borderRadius: 30, backgroundColor: '#00d4ff22', borderWidth: 1, borderColor: '#00d4ff' },
  btnTxt: { color: '#00d4ff', fontWeight: '800' },
  trackRow: { flexDirection: 'row', padding: 15, alignItems: 'center', borderBottomWidth: 1, borderBottomColor: '#0d1a2d' },
  trackArt: { width: 45, height: 45, backgroundColor: '#0d1a2d', borderRadius: 8, justifyContent: 'center', alignItems: 'center', marginRight: 15 },
  tTitle: { color: '#c0cce0', fontWeight: '600' },
  tSub: { color: '#4a5870', fontSize: 12 },
  nav: { flexDirection: 'row', backgroundColor: '#08111a', borderTopWidth: 1, borderTopColor: '#0d1a2d' },
  navBtn: { flex: 1, alignItems: 'center', paddingVertical: 15 },
  navTxt: { fontSize: 9, color: '#4a5870', marginTop: 4 },
  playerArt: { width: 250, height: 250, backgroundColor: '#0d1a2d', borderRadius: 20, justifyContent: 'center', alignItems: 'center', marginBottom: 30 },
  pTitle: { color: '#fff', fontSize: 22, fontWeight: '800' },
  controls: { flexDirection: 'row', alignItems: 'center', gap: 30, marginTop: 40 }
});
