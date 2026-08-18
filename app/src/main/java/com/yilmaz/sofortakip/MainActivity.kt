import React, { useState } from 'react';
import { 
  StyleSheet, 
  Text, 
  View, 
  ScrollView, 
  TouchableOpacity, 
  TextInput, 
  SafeAreaView, 
  Image 
} from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { 
  Car, 
  Navigation, 
  Clock, 
  Fuel, 
  FileText, 
  Settings, 
  BarChart2, 
  Plus, 
  Home, 
  History, 
  ChevronRight, 
  MapPin, 
  Sun 
} from 'lucide-react-native';

export default function App() {
  const [guzergah, setGuzergah] = useState('Akseki - Antalya');
  const [cikisKM, setCikisKM] = useState('125.430');
  const [cikisSaati, setCikisSaati] = useState('13:57');

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
        
        {/* Üst Başlık & Profil */}
        <View style={styles.header}>
          <View style={styles.userInfo}>
            <LinearGradient colors={['#7C3AED', '#2563EB']} style={styles.avatar}>
              <Car color="#FFF" size={24} />
            </LinearGradient>
            <View>
              <Text style={styles.appTitle}>Şoför Takip</Text>
              <Text style={styles.appSubtitle}>Günlük sefer paneli</Text>
            </View>
          </View>
          <View style={styles.headerActions}>
            <TouchableOpacity style={styles.headerBtn}><FileText size={16} color="#FFF" /><Text style={styles.btnText}>Notlar</Text></TouchableOpacity>
            <TouchableOpacity style={styles.headerBtn}><Text style={styles.btnText}>Tema</Text></TouchableOpacity>
          </View>
        </View>

        {/* Karşılama Kartı */}
        <LinearGradient colors={['#1E293B', '#0F172A']} style={styles.welcomeCard}>
          <View style={styles.welcomeText}>
            <View style={{flexDirection: 'row', alignItems: 'center', gap: 6}}>
              <Sun color="#F59E0B" size={20} />
              <Text style={styles.greeting}>Merhaba Yılmaz 👋</Text>
            </View>
            <Text style={styles.subGreeting}>Yola çıkmaya hazır mısın? Güvenli sürüşler dilerim...</Text>
          </View>
          <View style={styles.dateBadge}>
            <Text style={styles.dateNum}>15</Text>
            <Text style={styles.dateText}>Mayıs 2025</Text>
          </View>
        </LinearGradient>

        {/* Özet İstatistikler */}
        <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.statsContainer}>
          <View style={[styles.statBox, { backgroundColor: '#4F46E5' }]}>
            <Car color="#FFF" size={20} />
            <Text style={styles.statNum}>7</Text>
            <Text style={styles.statLabel}>Toplam Sefer</Text>
          </View>
          <View style={[styles.statBox, { backgroundColor: '#0284C7' }]}>
            <Navigation color="#FFF" size={20} />
            <Text style={styles.statNum}>3.850</Text>
            <Text style={styles.statLabel}>Toplam KM</Text>
          </View>
          <View style={[styles.statBox, { backgroundColor: '#059669' }]}>
            <Clock color="#FFF" size={20} />
            <Text style={styles.statNum}>32s 45dk</Text>
            <Text style={styles.statLabel}>Toplam Süre</Text>
          </View>
          <View style={[styles.statBox, { backgroundColor: '#D97706' }]}>
            <Fuel color="#FFF" size={20} />
            <Text style={styles.statNum}>₺1.925</Text>
            <Text style={styles.statLabel}>Yakıt Tahmini</Text>
          </View>
        </ScrollView>

        {/* Yeni Sefer Başlat Formu */}
        <LinearGradient colors={['#7C3AED', '#2563EB']} style={styles.formCard}>
          <Text style={styles.formTitle}>Yeni Sefer</Text>
          <Text style={styles.formSubtitle}>Sefer bilgilerinizi girin</Text>

          <View style={styles.inputContainer}>
            <View style={styles.inputRow}>
              <MapPin color="#7C3AED" size={20} />
              <View style={{ flex: 1, marginLeft: 10 }}>
                <Text style={styles.inputLabel}>Güzergâh</Text>
                <TextInput style={styles.inputText} value={guzergah} onChangeText={setGuzergah} />
              </View>
              <ChevronRight color="#64748B" size={20} />
            </View>

            <View style={styles.inputRow}>
              <Navigation color="#0284C7" size={20} />
              <View style={{ flex: 1, marginLeft: 10 }}>
                <Text style={styles.inputLabel}>Çıkış KM</Text>
                <TextInput style={styles.inputText} value={cikisKM} onChangeText={setCikisKM} keyboardType="numeric" />
              </View>
              <Text style={styles.unitText}>KM</Text>
            </View>

            <View style={styles.inputRow}>
              <Clock color="#EF4444" size={20} />
              <View style={{ flex: 1, marginLeft: 10 }}>
                <Text style={styles.inputLabel}>Çıkış Saati</Text>
                <TextInput style={styles.inputText} value={cikisSaati} onChangeText={setCikisSaati} />
              </View>
              <View style={styles.autoTag}><Text style={styles.autoTagText}>Otomatik</Text></View>
            </View>

            <TouchableOpacity style={styles.startBtn}>
              <LinearGradient colors={['#6D28D9', '#4C1D95']} style={styles.startBtnGradient}>
                <Car color="#FFF" size={20} />
                <Text style={styles.startBtnText}>SEFERİ BAŞLAT</Text>
              </LinearGradient>
            </TouchableOpacity>
          </View>
        </LinearGradient>

        {/* Sefer Geçmişi */}
        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>Sefer Geçmişi</Text>
          <TouchableOpacity><Text style={styles.seeAllText}>Tümünü Gör &gt;</Text></TouchableOpacity>
        </View>

        {[
          { id: 1, route: 'Akseki ➔ Antalya', date: '15.05.2025 • Perşembe', time: '08:15 ➔ 13:20', km: '550 KM', duration: '5s 05dk' },
          { id: 2, route: 'Antalya ➔ Akseki', date: '14.05.2025 • Çarşamba', time: '09:10 ➔ 13:15', km: '330 KM', duration: '4s 05dk' },
        ].map((item) => (
          <View key={item.id} style={styles.historyCard}>
            <View style={styles.historyInfo}>
              <Text style={styles.historyRoute}>{item.route}</Text>
              <Text style={styles.historyDate}>{item.date}</Text>
              <Text style={styles.historyDetails}>{item.time}</Text>
            </View>
            <View style={styles.historyStats}>
              <View style={styles.badgeSuccess}><Text style={styles.badgeSuccessText}>Tamamlandı ✓</Text></View>
              <Text style={styles.historyKm}>{item.km}</Text>
              <Text style={styles.historyDuration}>{item.duration}</Text>
            </View>
          </View>
        ))}

      </ScrollView>

      {/* Alt Navigasyon Barı */}
      <View style={styles.bottomNav}>
        <TouchableOpacity style={styles.navItem}><Home color="#7C3AED" size={22} /><Text style={[styles.navText, {color: '#7C3AED'}]}>Ana Sayfa</Text></TouchableOpacity>
        <TouchableOpacity style={styles.navItem}><History color="#64748B" size={22} /><Text style={styles.navText}>Geçmiş</Text></TouchableOpacity>
        
        <TouchableOpacity style={styles.fabBtn}>
          <LinearGradient colors={['#7C3AED', '#2563EB']} style={styles.fabGradient}>
            <Plus color="#FFF" size={28} />
          </LinearGradient>
        </TouchableOpacity>

        <TouchableOpacity style={styles.navItem}><BarChart2 color="#64748B" size={22} /><Text style={styles.navText}>Raporlar</Text></TouchableOpacity>
        <TouchableOpacity style={styles.navItem}><Settings color="#64748B" size={22} /><Text style={styles.navText}>Ayarlar</Text></TouchableOpacity>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#F1F5F9' },
  scrollContent: { padding: 16, paddingBottom: 100 },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 },
  userInfo: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  avatar: { width: 44, height: 44, borderRadius: 22, justifyContent: 'center', alignItems: 'center' },
  appTitle: { fontSize: 20, fontWeight: 'bold', color: '#0F172A' },
  appSubtitle: { fontSize: 12, color: '#64748B' },
  headerActions: { flexDirection: 'row', gap: 8 },
  headerBtn: { backgroundColor: '#1E293B', paddingHorizontal: 12, paddingVertical: 6, borderRadius: 20, flexDirection: 'row', alignItems: 'center', gap: 4 },
  btnText: { color: '#FFF', fontSize: 12, fontWeight: '500' },
  welcomeCard: { borderRadius: 16, padding: 16, flexDirection: 'row', justifyContent: 'space-between', marginBottom: 16 },
  greeting: { color: '#FFF', fontSize: 16, fontWeight: 'bold' },
  subGreeting: { color: '#94A3B8', fontSize: 12, marginTop: 4, maxWidth: '80%' },
  dateBadge: { backgroundColor: 'rgba(255,255,255,0.1)', padding: 8, borderRadius: 12, alignItems: 'center' },
  dateNum: { color: '#FFF', fontSize: 18, fontWeight: 'bold' },
  dateText: { color: '#94A3B8', fontSize: 10 },
  statsContainer: { flexDirection: 'row', marginBottom: 16 },
  statBox: { width: 110, padding: 12, borderRadius: 16, marginRight: 10, justifyContent: 'space-between' },
  statNum: { color: '#FFF', fontSize: 18, fontWeight: 'bold', marginVertical: 4 },
  statLabel: { color: 'rgba(255,255,255,0.8)', fontSize: 11 },
  formCard: { borderRadius: 20, padding: 16, marginBottom: 20 },
  formTitle: { color: '#FFF', fontSize: 22, fontWeight: 'bold' },
  formSubtitle: { color: '#E2E8F0', fontSize: 12, marginBottom: 12 },
  inputContainer: { backgroundColor: '#FFF', borderRadius: 16, padding: 12, gap: 10 },
  inputRow: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#F8FAFC', padding: 10, borderRadius: 12 },
  inputLabel: { fontSize: 10, color: '#64748B' },
  inputText: { fontSize: 14, fontWeight: '600', color: '#0F172A', padding: 0 },
  unitText: { color: '#64748B', fontWeight: 'bold', fontSize: 12 },
  autoTag: { backgroundColor: '#E0E7FF', paddingHorizontal: 8, paddingVertical: 4, borderRadius: 8 },
  autoTagText: { color: '#4338CA', fontSize: 10, fontWeight: '600' },
  startBtn: { marginTop: 6, borderRadius: 12, overflow: 'hidden' },
  startBtnGradient: { padding: 14, flexDirection: 'row', justifyContent: 'center', alignItems: 'center', gap: 8 },
  startBtnText: { color: '#FFF', fontWeight: 'bold', fontSize: 14 },
  sectionHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 },
  sectionTitle: { fontSize: 16, fontWeight: 'bold', color: '#0F172A' },
  seeAllText: { fontSize: 12, color: '#2563EB', fontWeight: '600' },
  historyCard: { backgroundColor: '#FFF', borderRadius: 16, padding: 12, flexDirection: 'row', justifyContent: 'space-between', marginBottom: 10 },
  historyRoute: { fontSize: 14, fontWeight: 'bold', color: '#0F172A' },
  historyDate: { fontSize: 11, color: '#64748B', marginVertical: 2 },
  historyDetails: { fontSize: 11, color: '#94A3B8' },
  historyStats: { alignItems: 'flex-end', justifyContent: 'space-between' },
  badgeSuccess: { backgroundColor: '#DCFCE7', paddingHorizontal: 8, paddingVertical: 2, borderRadius: 10 },
  badgeSuccessText: { color: '#166534', fontSize: 10, fontWeight: '600' },
  historyKm: { fontSize: 14, fontWeight: 'bold', color: '#0F172A' },
  historyDuration: { fontSize: 11, color: '#64748B' },
  bottomNav: { position: 'absolute', bottom: 0, left: 0, right: 0, backgroundColor: '#FFF', height: 65, flexDirection: 'row', justifyContent: 'space-around', alignItems: 'center', borderTopWidth: 1, borderTopColor: '#E2E8F0' },
  navItem: { alignItems: 'center' },
  navText: { fontSize: 10, color: '#64748B', marginTop: 2 },
  fabBtn: { top: -20 },
  fabGradient: { width: 52, height: 52, borderRadius: 26, justifyContent: 'center', alignItems: 'center', elevation: 5 }
});
