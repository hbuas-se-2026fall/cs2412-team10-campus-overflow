import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import QuestionListPage from './pages/QuestionListPage';
import NewQuestionPage from './pages/NewQuestionPage';

export default function App() {
  return (
    <BrowserRouter>
      <div style={{ padding: '20px', fontFamily: 'sans-serif' }}>
        <header
          style={{ marginBottom: '20px', paddingBottom: '10px', borderBottom: '1px solid #ccc' }}
        >
          <h1>CampusOverflow</h1>
          <nav>
            <Link to="/questions" style={{ marginRight: '10px' }}>
              问题列表
            </Link>
            <Link to="/questions/new">我要提问</Link>
          </nav>
        </header>
        <main>
          <Routes>
            <Route path="/" element={<QuestionListPage />} />
            <Route path="/questions" element={<QuestionListPage />} />
            <Route path="/questions/new" element={<NewQuestionPage />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}
